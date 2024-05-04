package com.tac.guns.client.animation;

import java.util.Arrays;

public class AnimationChannelContent {
    public float[] keyframeTimeS;
    /**
     * The values. Each element of this array corresponds to one key frame time,
     * can be value of translation, rotation or scale
     */
    public float[][] values;

    public AnimationChannelContent() {
    }

    public AnimationChannelContent(AnimationChannelContent source) {
        if (source.keyframeTimeS != null) {
            this.keyframeTimeS = Arrays.copyOf(source.keyframeTimeS, source.keyframeTimeS.length);
        }
        if (source.values != null) {
            // deep copy animation values
            this.values = Arrays.stream(source.values)
                    .map(values -> Arrays.copyOf(values, values.length))
                    .toArray(float[][]::new);
        }
    }

    public enum LerpMode{
        LINEAR, SPHERICAL_LINEAR, CATMULL_ROM
    }
}
