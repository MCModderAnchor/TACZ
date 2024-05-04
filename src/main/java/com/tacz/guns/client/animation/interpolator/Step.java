package com.tacz.guns.client.animation.interpolator;

import com.tacz.guns.client.animation.AnimationChannelContent;

public class Step implements Interpolator {
    private AnimationChannelContent content;

    @Override
    public void compile(AnimationChannelContent content) {
        this.content = content;
    }

    @Override
    public void interpolate(int indexFrom, int indexTo, float alpha, float[] result) {
        for (int i = 0; i < content.values[indexFrom].length; i++) {
            if (alpha < 1)
                result[i] = content.values[indexFrom][i];
            else
                result[i] = content.values[indexTo][i];
        }
    }

    @Override
    public Step clone() {
        try {
            Step step = (Step) super.clone();
            step.content = this.content;
            return step;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
