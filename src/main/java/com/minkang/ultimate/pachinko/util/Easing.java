package com.minkang.ultimate.pachinko.util;

public class Easing {
    // t: 0..1
    public static double easeInOutQuad(double t){
        if (t < 0.5) return 2 * t * t;
        return -1 + (4 - 2 * t) * t;
    }
    public static double clamp01(double v){
        if (v < 0) return 0;
        if (v > 1) return 1;
        return v;
    }
}
