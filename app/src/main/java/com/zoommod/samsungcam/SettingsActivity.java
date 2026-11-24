package com.zoommod.samsungcam;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/** Layar pengaturan sederhana. Nilai dibaca live oleh hook di proses kamera (refresh tiap ~2 detik). */
public class SettingsActivity extends Activity {

    private SharedPreferences sp;
    private boolean worldReadable = true;

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
        sp = openPrefs();

        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad * 3, pad, pad);
        scroll.addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(this);
        title.setText("Samsung Cam Zoom Mod");
        title.setTextSize(22);
        root.addView(title);

        TextView hint = new TextView(this);
        hint.setText("Aktifkan modul di LSPosed (scope: Kamera), lalu Force Stop kamera. "
                + "Perubahan di sini terbaca otomatis oleh kamera.");
        hint.setPadding(0, pad / 2, 0, pad);
        root.addView(hint);

        addSwitch(root, "Smooth zoom", Cfg.K_SMOOTH_ON, Cfg.DEF_SMOOTH_ON);
        addSeek(root, "Durasi easing", Cfg.K_SMOOTH_MS, 50, 600, Cfg.DEF_SMOOTH_MS, false, " ms");

        addSwitch(root, "Naikkan batas zoom maksimum", Cfg.K_MAX_ON, Cfg.DEF_MAX_ON);
        addSeek(root, "Zoom maksimum", Cfg.K_MAX_ZOOM, 30, 100, Cfg.DEF_MAX_ZOOM, true, "x");

        TextView note = new TextView(this);
        note.setText("Catatan: di atas 30x hasil bergantung pada HAL kamera HP. "
                + "Kalau preview macet, matikan opsi batas zoom.");
        note.setPadding(0, pad, 0, 0);
        root.addView(note);

        setContentView(scroll);

        if (!worldReadable) {
            Toast.makeText(this, "Modul belum aktif di LSPosed, pengaturan belum bisa dibaca kamera",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void addSwitch(LinearLayout root, String label, final String key, boolean def) {
        Switch sw = new Switch(this);
        sw.setText(label);
        sw.setChecked(sp.getBoolean(key, def));
        sw.setPadding(0, 24, 0, 24);
        sw.setOnCheckedChangeListener((v, on) -> sp.edit().putBoolean(key, on).apply());
        root.addView(sw);
    }

    private void addSeek(LinearLayout root, final String label, final String key,
                         final int min, final int max, int def,
                         final boolean asFloat, final String unit) {
        int cur = asFloat ? Math.round(sp.getFloat(key, def)) : sp.getInt(key, def);
        cur = Math.max(min, Math.min(max, cur));

        final TextView tv = new TextView(this);
        tv.setText(label + ": " + cur + unit);
        root.addView(tv);

        SeekBar sb = new SeekBar(this);
        sb.setMax(max - min);
        sb.setProgress(cur - min);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                int v = progress + min;
                tv.setText(label + ": " + v + unit);
                if (!fromUser) return;
                if (asFloat) sp.edit().putFloat(key, v).apply();
                else sp.edit().putInt(key, v).apply();
            }

            @Override
            public void onStartTrackingTouch(SeekBar s) {}

            @Override
            public void onStopTrackingTouch(SeekBar s) {}
        });
        root.addView(sb);
    }
}
