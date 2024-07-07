package com.tacz.guns.api.client.animation.interpolator;

import com.tacz.guns.api.client.animation.AnimationChannelContent;

public class Spline implements Interpolator {
    @Override
    public void compile(AnimationChannelContent content) {
        // TODO
    }

    @Override
    public float[] interpolate(int indexFrom, int indexTo, float alpha) {
        // TODO
        return new float[]{0, 0, 0, 1};
    }

    @Override
    public Interpolator clone() {
        // TODO
        return null;
    }
}
