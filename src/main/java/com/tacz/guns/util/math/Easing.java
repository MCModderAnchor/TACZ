package com.tacz.guns.util.math;

public class Easing {
    public static double easeOutCubic(double x) {
        return 1 - Math.pow(1 - x, 3);
    }
}
