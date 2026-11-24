package com.zoommod.samsungcam;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import java.lang.ref.WeakReference;
import java.lang.reflect.Member;
import java.util.Map;
import java.util.WeakHashMap;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Hook untuk kamera bawaan Samsung (com.sec.android.app.camera, One UI 8 / v16.0.00.66).
 *
 * 1) Batas zoom maksimum: CapabilityImpl.getScalerAvailableMaxDigitalZoom(ZoomType, Resolution)
 *    -> hasilnya dinaikkan. Nilai zoom internal = rasio x 1000 (ZoomController.getMaxZoomLevel).
 * 2) Smooth zoom: ZoomController.setZoomLevel(int) -> nilai target dihaluskan (easing eksponensial
 *    pada skala logaritmik) lalu diteruskan ke method asli sedikit demi sedikit.
 */
public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "[SamsungCamZoom] ";
    private static final String TARGET_PKG = "com.sec.android.app.camera";
    private static final String MOD_PKG = "com.zoommod.samsungcam";
    private static final String P = "com.sec.android.app.camera.";

    private static XSharedPreferences prefs;
    private static long lastReload;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!TARGET_PKG.equals(lpparam.packageName)) return;
        XposedBridge.log(TAG + "memasang hook di " + lpparam.packageName);

        try {
            hookMaxZoom(lpparam.classLoader);
        } catch (Throwable t) {
            XposedBridge.log(TAG + "hook max zoom gagal: " + t);
        }
        try {
            hookSmoothZoom(lpparam.classLoader);
        } catch (Throwable t) {
            XposedBridge.log(TAG + "hook smooth zoom gagal: " + t);
        }
    }

    // ---------------------------------------------------------------- pengaturan

    private static synchronized XSharedPreferences prefs() {
        long now = SystemClock.elapsedRealtime();
        if (prefs == null) {
            prefs = new XSharedPreferences(MOD_PKG, Cfg.PREFS);
            lastReload = now;
        } else if (now - lastReload > 2000) {
            prefs.reload();
            lastReload = now;
        }
        return prefs;
    }

    // ---------------------------------------------------------------- max zoom

    private void hookMaxZoom(ClassLoader cl) {
        Class<?> capImpl = XposedHelpers.findClass(P + "engine.core.CapabilityImpl", cl);
        Class<?> zoomType = XposedHelpers.findClass(P + "interfaces.ZoomType", cl);
        Class<?> resolution = XposedHelpers.findClass(P + "interfaces.Resolution", cl);

        XposedHelpers.findAndHookMethod(capImpl, "getScalerAvailableMaxDigitalZoom",
                zoomType, resolution, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            XSharedPreferences p = prefs();
                            if (!p.getBoolean(Cfg.K_MAX_ON, Cfg.DEF_MAX_ON)) return;

                            String type = String.valueOf(param.args[0]);
                            if (!"NORMAL".equals(type) && !"VIDEO".equals(type)) return;

                            Object r = param.getResult();
                            if (!(r instanceof Float)) return;
                            float orig = (Float) r;
                            // hanya kamera belakang yang aslinya punya zoom jauh (>= 5x)
                            if (orig < 5f) return;

                            float want = p.getFloat(Cfg.K_MAX_ZOOM, Cfg.DEF_MAX_ZOOM);
                            if (want > 100f) want = 100f; // data kurva zoom Samsung berhenti di 100x
                            if (want > orig) param.setResult(Float.valueOf(want));
                        } catch (Throwable t) {
                            XposedBridge.log(TAG + "max zoom: " + t);
                        }
                    }
                });
        XposedBridge.log(TAG + "hook max zoom terpasang");
    }

    // ---------------------------------------------------------------- smooth zoom

    private void hookSmoothZoom(ClassLoader cl) {
        Class<?> zc = XposedHelpers.findClass(P + "engine.ZoomController", cl);

        XposedHelpers.findAndHookMethod(zc, "setZoomLevel", int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                try {
                    XSharedPreferences p = prefs();
                    if (!p.getBoolean(Cfg.K_SMOOTH_ON, Cfg.DEF_SMOOTH_ON)) return;
                    int ms = p.getInt(Cfg.K_SMOOTH_MS, Cfg.DEF_SMOOTH_MS);
                    if (ms < 30) return;

                    Smoother s = Smoother.of(param.thisObject, param.method);
                    int level = (Integer) param.args[0];
                    if (s.submit(level, ms)) {
                        param.setResult(null); // lewati pemanggilan asli, animator yang memanggilnya
                    }
                } catch (Throwable t) {
                    XposedBridge.log(TAG + "smooth zoom: " + t);
                }
            }
        });
        XposedBridge.log(TAG + "hook smooth zoom terpasang");
    }

    /**
     * Menghaluskan perubahan zoom. Nilai zoom Samsung = rasio x 1000 (500 = 0.5x, 30000 = 30x).
     * Easing dilakukan di skala logaritmik supaya terasa rata dari 0.5x sampai 100x.
     */
    private static final class Smoother implements Runnable {
        private static final Map<Object, Smoother> INSTANCES = new WeakHashMap<>();
        private static final Handler MAIN = new Handler(Looper.getMainLooper());
        /** Jeda lebih lama dari ini = zoom diloncatkan langsung (ganti kamera/mode), tanpa animasi. */
        private static final long SNAP_AFTER_MS = 800;
        private static final long FRAME_MS = 12;

        private final WeakReference<Object> owner;
        private final Member method;

        private double curLn = Double.NaN;
        private double tgtLn;
        private int tgtLevel;
        private int durationMs = Cfg.DEF_SMOOTH_MS;
        private long lastCall;
        private long lastTick;
        private boolean running;

        private Smoother(Object owner, Member method) {
            this.owner = new WeakReference<>(owner);
            this.method = method;
        }

        static synchronized Smoother of(Object owner, Member method) {
            Smoother s = INSTANCES.get(owner);
            if (s == null) {
                s = new Smoother(owner, method);
                INSTANCES.put(owner, s);
            }
            return s;
        }

        /** @return true jika pemanggilan asli harus dilewati (animator yang akan memanggilnya). */
        synchronized boolean submit(int level, int ms) {
            long now = SystemClock.uptimeMillis();
            durationMs = ms;
            tgtLevel = level;
            tgtLn = Math.log(Math.max(level, 1));

            boolean idle = (now - lastCall) > SNAP_AFTER_MS;
            lastCall = now;

            if (Double.isNaN(curLn) || (idle && !running)) {
                curLn = tgtLn;
                return false; // biarkan pemanggilan asli jalan
            }
            if (!running) {
                running = true;
                lastTick = now;
                MAIN.post(this);
            }
            return true;
        }

        @Override
        public void run() {
            Object o = owner.get();
            if (o == null) {
                synchronized (this) {
                    running = false;
                }
                return;
            }

            int level;
            boolean done;
            synchronized (this) {
                long now = SystemClock.uptimeMillis();
                double dt = Math.max(1, now - lastTick);
                lastTick = now;
                double tau = Math.max(10.0, durationMs / 3.0);
                double k = 1.0 - Math.exp(-dt / tau);

                curLn += (tgtLn - curLn) * k;
                done = Math.abs(tgtLn - curLn) < 0.003;
                if (done) {
                    curLn = tgtLn;
                    level = tgtLevel;
                    running = false;
                } else {
                    level = (int) Math.round(Math.exp(curLn));
                }
            }

            try {
                XposedBridge.invokeOriginalMethod(method, o, new Object[]{Integer.valueOf(level)});
            } catch (Throwable t) {
                XposedBridge.log(TAG + "invoke setZoomLevel: " + t);
            }
            if (!done) MAIN.postDelayed(this, FRAME_MS);
        }
    }
}
