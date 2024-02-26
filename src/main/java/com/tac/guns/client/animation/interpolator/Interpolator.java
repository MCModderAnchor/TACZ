package com.tac.guns.client.animation.interpolator;

import com.tac.guns.client.animation.ObjectAnimationChannel;

public interface Interpolator {
    void compile(ObjectAnimationChannel channel);

    void interpolate(int indexFrom, int indexTo, float alpha, float[] result);
}
