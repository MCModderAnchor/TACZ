package com.tac.guns.client.animation.interpolator;

import com.tac.guns.client.animation.ObjectAnimationChannel;

public class Step implements Interpolator {
    private ObjectAnimationChannel channel;

    @Override
    public void compile(ObjectAnimationChannel channel) {
        this.channel = channel;
    }

    @Override
    public void interpolate(int indexFrom, int indexTo, float alpha, float[] result) {
        for (int i = 0; i < channel.content.values[indexFrom].length; i++) {
            if (alpha < 1)
                result[i] = channel.content.values[indexFrom][i];
            else
                result[i] = channel.content.values[indexTo][i];
        }
    }
}
