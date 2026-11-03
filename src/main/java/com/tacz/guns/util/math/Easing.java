package com.tacz.guns.util.math;

public class Easing {
    public static double easeInExpo(double x) {
        if (x <= 0) {
            return 0;
        }
        if (x >= 1) {
            return 1;
        }
        return Math.pow(2, 10 * x - 10);
    }

    public static double easeOutCubic(double x) {
        return 1 - Math.pow(1 - x, 3);
    }
}
