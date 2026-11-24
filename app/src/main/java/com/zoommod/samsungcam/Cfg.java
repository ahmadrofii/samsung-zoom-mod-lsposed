package com.zoommod.samsungcam;

/** Kunci dan nilai default pengaturan, dipakai oleh hook dan layar pengaturan. */
final class Cfg {
    static final String PREFS = "settings";

    static final String K_MAX_ON = "max_on";
    static final String K_MAX_ZOOM = "max_zoom";      // float, kelipatan (x)
    static final String K_SMOOTH_ON = "smooth_on";
    static final String K_SMOOTH_MS = "smooth_ms";    // int, durasi easing (ms)

    static final boolean DEF_MAX_ON = true;
    static final int DEF_MAX_ZOOM = 100;               // kurva zoom bawaan Samsung sampai 100x
    static final boolean DEF_SMOOTH_ON = true;
    static final int DEF_SMOOTH_MS = 220;

    private Cfg() {}
}
