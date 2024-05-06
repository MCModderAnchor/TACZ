package com.tacz.guns.client.animation.interpolator;

import com.tacz.guns.client.animation.AnimationChannelContent;

public class Linear implements Interpolator {
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
            if (indexFrom == indexTo) {
                result[i] = content.values[indexFrom][i + offset];
            } else {
                result[i] = content.values[indexFrom][i + offset] * (1 - alpha) + content.values[indexTo][i] * alpha;
            }
        }
    }

    @Override
    public Linear clone() {
        try {
            Linear linear = (Linear) super.clone();
            linear.content = this.content;
            return linear;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
