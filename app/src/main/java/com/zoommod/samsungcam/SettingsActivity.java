package com.zoommod.samsungcam;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Layar pengaturan bergaya gelap: kartu membulat, saklar pil, slider tebal.
 * Nilai dibaca live oleh hook di proses kamera (refresh tiap ~2 detik).
 */
public class SettingsActivity extends Activity {

    private static final int BG = 0xFF000000;
    private static final int CARD = 0xFF1A1A1A;
    private static final int CARD_STROKE = 0xFF2A2A2A;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int SUB = 0xFF9A9A9F;
    private static final int SECTION = 0xFF8E8E93;
    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    private SharedPreferences sp;
    private boolean worldReadable = true;
    private float density;

    private int dp(float v) {
        return Math.round(v * density);
    }

    @SuppressLint("WorldReadableFiles")
    @SuppressWarnings("deprecation")
    private SharedPreferences openPrefs() {
        try {
            return getSharedPreferences(Cfg.PREFS, MODE_WORLD_READABLE);
        } catch (SecurityException e) {
            worldReadable = false;
            return getSharedPreferences(Cfg.PREFS, MODE_PRIVATE);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        density = getResources().getDisplayMetrics().density;
        sp = openPrefs();

        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(28), dp(20), dp(40));
        scroll.addView(root, new ViewGroup.LayoutParams(MATCH, WRAP));

        root.addView(text("Samsung Cam Zoom Mod", 30, TEXT, true));
        TextView ver = text("v1.0 \u00B7 Kamera bawaan Samsung (One UI 8)", 16, SUB, false);
        ver.setPadding(0, dp(6), 0, dp(8));
        root.addView(ver);

        addSection(root, "Umum");
        LinearLayout c1 = addCard(root);
        addSwitchRow(c1, "Aktifkan modul", "Saklar utama semua fitur mod",
                Cfg.K_MASTER, Cfg.DEF_MASTER);

        addSection(root, "Zoom Kamera");
        LinearLayout c2 = addCard(root);
        addSwitchRow(c2, "Smooth zoom", "Animasi zoom halus (ease-out)",
                Cfg.K_SMOOTH_ON, Cfg.DEF_SMOOTH_ON);
        addDivider(c2);
        addSliderRow(c2, "Durasi zoom", "Makin tinggi makin lambat dan mulus",
                Cfg.K_SMOOTH_MS, 50, 600, Cfg.DEF_SMOOTH_MS, false, " ms");

        addSection(root, "Batas Zoom");
        LinearLayout c3 = addCard(root);
        addSwitchRow(c3, "Buka limit zoom", "Naikkan batas zoom maksimum kamera belakang",
                Cfg.K_MAX_ON, Cfg.DEF_MAX_ON);
        addDivider(c3);
        addSliderRow(c3, "Zoom maksimum", "Di atas 30x tergantung kemampuan kamera HP",
                Cfg.K_MAX_ZOOM, 30, 100, Cfg.DEF_MAX_ZOOM, true, "x");

        TextView note = text("Aktifkan modul di LSPosed (scope: Kamera), lalu Force Stop kamera. "
                + "Perubahan di sini terbaca otomatis oleh kamera.", 13, SUB, false);
        note.setPadding(dp(6), dp(24), dp(6), 0);
        root.addView(note);

        setContentView(scroll);

        if (!worldReadable) {
            Toast.makeText(this,
                    "Modul belum aktif di LSPosed, pengaturan belum bisa dibaca kamera",
                    Toast.LENGTH_LONG).show();
        }
    }

    // ------------------------------------------------------------ builder UI

    private TextView text(String s, float sp, int color, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(s);
        tv.setTextSize(sp);
        tv.setTextColor(color);
        if (bold) tv.setTypeface(Typeface.DEFAULT_BOLD);
        return tv;
    }

    private void addSection(LinearLayout root, String title) {
        TextView tv = text(title, 16, SECTION, true);
        tv.setPadding(dp(6), dp(28), 0, dp(10));
        root.addView(tv);
    }

    private LinearLayout addCard(LinearLayout root) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD);
        bg.setCornerRadius(dp(28));
        bg.setStroke(dp(1), CARD_STROKE);
        card.setBackground(bg);
        card.setPadding(dp(22), dp(8), dp(22), dp(8));
        root.addView(card, new LinearLayout.LayoutParams(MATCH, WRAP));
        return card;
    }

    private void addDivider(LinearLayout card) {
        View line = new View(this);
        line.setBackgroundColor(CARD_STROKE);
        card.addView(line, new LinearLayout.LayoutParams(MATCH, dp(1)));
    }

    private void addSwitchRow(LinearLayout card, String title, String sub,
                              final String key, boolean def) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(16), 0, dp(16));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.addView(text(title, 20, TEXT, true));
        TextView s = text(sub, 15, SUB, false);
        s.setPadding(0, dp(2), 0, 0);
        texts.addView(s);
        row.addView(texts, new LinearLayout.LayoutParams(0, WRAP, 1f));

        final PillSwitch sw = new PillSwitch(this, sp.getBoolean(key, def));
        sw.setListener(on -> sp.edit().putBoolean(key, on).apply());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(WRAP, WRAP);
        lp.leftMargin = dp(12);
        row.addView(sw, lp);

        row.setOnClickListener(v -> sw.performClick());
        card.addView(row, new LinearLayout.LayoutParams(MATCH, WRAP));
    }

    private void addSliderRow(LinearLayout card, String title, String sub,
                              final String key, final int min, final int max, final int def,
                              final boolean asFloat, final String unit) {
        int cur = asFloat ? Math.round(sp.getFloat(key, def)) : sp.getInt(key, def);
        cur = Math.max(min, Math.min(max, cur));

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, dp(16), 0, dp(20));

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(text(title, 20, TEXT, true), new LinearLayout.LayoutParams(0, WRAP, 1f));

        final TextView value = text(cur + unit, 17, SUB, false);
        head.addView(value);

        TextView reset = text("\u21BB", 24, SUB, false);
        reset.setPadding(dp(12), dp(4), dp(4), dp(4));
        head.addView(reset);
        box.addView(head);

        TextView s = text(sub, 15, SUB, false);
        s.setPadding(0, dp(2), 0, 0);
        box.addView(s);

        final PillSlider slider = new PillSlider(this, min, max, cur);
        slider.setListener((val, fromUser) -> {
            value.setText(val + unit);
            if (fromUser) save(key, val, asFloat);
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, dp(40));
        lp.topMargin = dp(16);
        box.addView(slider, lp);

        reset.setOnClickListener(v -> {
            slider.setValue(def);
            value.setText(def + unit);
            save(key, def, asFloat);
        });

        card.addView(box, new LinearLayout.LayoutParams(MATCH, WRAP));
    }

    private void save(String key, int v, boolean asFloat) {
        SharedPreferences.Editor e = sp.edit();
        if (asFloat) e.putFloat(key, v);
        else e.putInt(key, v);
        e.apply();
    }

    // ------------------------------------------------------------ saklar pil

    static final class PillSwitch extends View {
        interface Listener {
            void onToggled(boolean on);
        }

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private final float density;
        private boolean checked;
        private float progress;
        private ValueAnimator anim;
        private Listener listener;

        PillSwitch(Context c, boolean checked) {
            super(c);
            this.density = c.getResources().getDisplayMetrics().density;
            this.checked = checked;
            this.progress = checked ? 1f : 0f;
            setClickable(true);
            setOnClickListener(v -> toggle());
        }

        void setListener(Listener l) {
            listener = l;
        }

        private void toggle() {
            checked = !checked;
            if (anim != null) anim.cancel();
            anim = ValueAnimator.ofFloat(progress, checked ? 1f : 0f);
            anim.setDuration(180);
            anim.addUpdateListener(a -> {
                progress = (Float) a.getAnimatedValue();
                invalidate();
            });
            anim.start();
            if (listener != null) listener.onToggled(checked);
        }

        private static int blend(int a, int b, float t) {
            int ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
            int br = (b >> 16) & 255, bgc = (b >> 8) & 255, bb = b & 255;
            int r = Math.round(ar + (br - ar) * t);
            int g = Math.round(ag + (bgc - ag) * t);
            int bl = Math.round(ab + (bb - ab) * t);
            return 0xFF000000 | (r << 16) | (g << 8) | bl;
        }

        @Override
        protected void onMeasure(int widthSpec, int heightSpec) {
            setMeasuredDimension(Math.round(52 * density), Math.round(32 * density));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float w = getWidth();
            float h = getHeight();

            paint.setColor(blend(0xFF3A3A3C, 0xFFFFFFFF, progress));
            rect.set(0, 0, w, h);
            canvas.drawRoundRect(rect, h / 2f, h / 2f, paint);

            float r = h * 0.30f;
            float cx = h / 2f + progress * (w - h);
            paint.setColor(blend(0xFF9A9A9F, 0xFFFFFFFF, progress));
            canvas.drawCircle(cx, h / 2f, r, paint);
        }
    }

    // ------------------------------------------------------------ slider tebal

    static final class PillSlider extends View {
        interface Listener {
            void onChanged(int value, boolean fromUser);
        }

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private final float density;
        private final int min;
        private final int max;
        private int value;
        private Listener listener;

        PillSlider(Context c, int min, int max, int value) {
            super(c);
            this.density = c.getResources().getDisplayMetrics().density;
            this.min = min;
            this.max = max;
            this.value = Math.max(min, Math.min(max, value));
        }

        void setListener(Listener l) {
            listener = l;
        }

        void setValue(int v) {
            value = Math.max(min, Math.min(max, v));
            invalidate();
        }

        @Override
        protected void onMeasure(int widthSpec, int heightSpec) {
            setMeasuredDimension(resolveSize(Math.round(200 * density), widthSpec),
                    Math.round(40 * density));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float w = getWidth();
            float h = getHeight();
            float frac = (max == min) ? 0f : (value - min) / (float) (max - min);
            float cx = h / 2f + frac * (w - h);

            // trek (bagian kosong)
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xFF2C2C2E);
            rect.set(0, 0, w, h);
            canvas.drawRoundRect(rect, h / 2f, h / 2f, paint);

            // bagian terisi (putih) sampai pusat thumb
            canvas.save();
            canvas.clipRect(0, 0, cx, h);
            paint.setColor(0xFFFFFFFF);
            canvas.drawRoundRect(rect, h / 2f, h / 2f, paint);
            canvas.restore();

            // thumb: lingkaran putih dengan cincin tipis
            float r = h * 0.46f;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xFFFFFFFF);
            canvas.drawCircle(cx, h / 2f, r, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1.5f * density);
            paint.setColor(0xFFBDBDBD);
            canvas.drawCircle(cx, h / 2f, r - paint.getStrokeWidth() / 2f, paint);
            paint.setStyle(Paint.Style.FILL);
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                    update(e.getX());
                    return true;
                case MotionEvent.ACTION_MOVE:
                    update(e.getX());
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
                    return true;
                default:
                    return super.onTouchEvent(e);
            }
        }

        private void update(float x) {
            float h = getHeight();
            float w = getWidth();
            if (w <= h) return;
            float f = (x - h / 2f) / (w - h);
            f = Math.max(0f, Math.min(1f, f));
            int v = min + Math.round(f * (max - min));
            if (v != value) {
                value = v;
                invalidate();
                if (listener != null) listener.onChanged(v, true);
            }
        }
    }
}
