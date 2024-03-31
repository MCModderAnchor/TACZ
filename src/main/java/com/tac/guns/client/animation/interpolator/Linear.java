package com.tac.guns.client.animation.interpolator;

import com.tac.guns.client.animation.AnimationChannelContent;

public class Linear implements Interpolator {
    private AnimationChannelContent content;

    @Override
    public void compile(AnimationChannelContent content) {
        this.content = content;
    }

    @Override
    public void interpolate(int indexFrom, int indexTo, float alpha, float[] result) {
        for (int i = 0; i < content.values[indexFrom].length; i++) {
            result[i] = content.values[indexFrom][i] * (1 - alpha) + content.values[indexTo][i] * alpha;
        }
    }
}
