package com.tacz.guns.api.client.animation.interpolator;

import com.tacz.guns.api.client.animation.AnimationChannelContent;

public interface Interpolator extends Cloneable {
    void compile(AnimationChannelContent content);

    float[] interpolate(int indexFrom, int indexTo, float alpha);

    Interpolator clone();
}
