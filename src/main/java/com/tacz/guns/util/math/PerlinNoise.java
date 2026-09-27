package com.tacz.guns.util.math;

import com.tacz.guns.util.time.GunTime;

import java.util.Random;

public class PerlinNoise {
    private final Random random = new Random();
    private final float rangeDown;
    private final float rangeUp;
    private final long periodMs;
    private float prevNum;
    private float num;
    private long prevTime = GunTime.nowMillis();
    private long epoch = GunTime.epoch();
    private boolean reverse = false;

    public PerlinNoise(float rangeDown, float rangeUp, long periodMs) {
        this.rangeDown = rangeDown;
        this.rangeUp = rangeUp;
        this.periodMs = periodMs;
        prevNum = random.nextFloat() * (rangeUp - rangeDown) + rangeDown;
        num = random.nextFloat() * (rangeUp - rangeDown) + rangeDown;
        if (reverse && prevNum * num > 0) {
            num = -num;
        }
    }

    private static double easeInterpolate(double x) {
        return (3 * Math.pow(x, 2) - 2 * Math.pow(x, 3));
    }

    public void setReverse(boolean reverse) {
        this.reverse = reverse;
    }

    public float getValue() {
        long currentEpoch = GunTime.epoch();
        long now = GunTime.nowMillis();
        if (epoch != currentEpoch || now < prevTime) {
            epoch = currentEpoch;
            prevTime = now;
            prevNum = num;
            num = random.nextFloat() * (rangeUp - rangeDown) + rangeDown;
            if (reverse && prevNum * num > 0) {
                num = -num;
            }
        }
        long periodTime = now - prevTime;
        long repeat = periodTime / periodMs;
        long partialTime = periodTime % periodMs;
        prevTime += repeat * periodMs;
        double x = easeInterpolate((double) partialTime / (double) periodMs);
        if (repeat == 1) {
            prevNum = num;
            num = random.nextFloat() * (rangeUp - rangeDown) + rangeDown;
            if (reverse && prevNum * num > 0) {
                num = -num;
            }
        } else if (repeat > 1) {
            prevNum = random.nextFloat() * (rangeUp - rangeDown) + rangeDown;
            num = random.nextFloat() * (rangeUp - rangeDown) + rangeDown;
            if (reverse && prevNum * num > 0) {
                num = -num;
            }
        }
        return (float) (prevNum * (1 - x) + num * x);
    }
}
