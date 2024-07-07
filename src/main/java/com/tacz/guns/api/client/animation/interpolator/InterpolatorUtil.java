package com.tacz.guns.api.client.animation.interpolator;

public class InterpolatorUtil {
    public static Interpolator fromInterpolation(InterpolatorType interpolation) {
        switch (interpolation) {
            case SPLINE -> {
                return new Spline();
            }
            case STEP -> {
                return new Step();
            }
            case SLERP -> {
                return new SLerp();
            }
            default -> {
                return new Linear();
            }
        }
    }

    public enum InterpolatorType {
        LINEAR,
        SLERP,
        SPLINE,
        STEP
    }
}
