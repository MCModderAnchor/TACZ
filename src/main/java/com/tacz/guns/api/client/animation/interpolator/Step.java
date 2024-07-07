package com.tacz.guns.api.client.animation.interpolator;

import com.tacz.guns.api.client.animation.AnimationChannelContent;

import java.util.Arrays;

public class Step implements Interpolator {
    private AnimationChannelContent content;

    @Override
    public void compile(AnimationChannelContent content) {
        this.content = content;
    }

    @Override
    public float[] interpolate(int indexFrom, int indexTo, float alpha) {
        float[] result;
        int offset = switch (content.values[indexFrom].length) {
            case 8 -> 4;
            case 6 -> 3;
            default -> 0;
        };
        if (alpha < 1 || indexFrom == indexTo) {
            result = Arrays.copyOfRange(content.values[indexFrom], offset, content.values[indexFrom].length);
        } else {
            int length = content.values[indexTo].length;
            length = switch (length) {
                case 8 -> 4;
                case 6 -> 3;
                default -> length;
            };
            result = Arrays.copyOfRange(content.values[indexTo], 0, length);
        }
        return result;
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
