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
        // 如果动画值有 6 个，后三个为 Post 数值，用于插值起点
        int offset = content.values[indexFrom].length == 6 ? 3 : 0;
        for (int i = 0; i < result.length; i++) {
            if (alpha < 1 || indexFrom == indexTo) {
                result[i] = content.values[indexFrom][i + offset];
            } else {
                result[i] = content.values[indexTo][i];
            }
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
