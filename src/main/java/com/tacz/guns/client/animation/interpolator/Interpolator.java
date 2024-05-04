package com.tacz.guns.client.animation.interpolator;

import com.tacz.guns.client.animation.AnimationChannelContent;

public interface Interpolator extends Cloneable {
    void compile(AnimationChannelContent content);

    void interpolate(int indexFrom, int indexTo, float alpha, float[] result);

    Interpolator clone();
}
