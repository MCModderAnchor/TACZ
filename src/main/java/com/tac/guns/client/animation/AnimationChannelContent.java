package com.tac.guns.client.animation;

import com.tac.guns.client.animation.interpolator.Interpolator;

public class AnimationChannelContent {
    public float[] keyframeTimeS;
    /**
     * The values. Each element of this array corresponds to one key frame time,
     * can be value of translation, rotation or scale
     */
    public float[][] values;

    public Interpolator interpolator;
}
