package com.tac.guns.client.animation.interpolator;

import com.tac.guns.client.animation.ObjectAnimationChannel;

/**
 * 用于四元数的线性插值。
 */
public class SLerp implements Interpolator {
    private ObjectAnimationChannel channel;

    @Override
    public void compile(ObjectAnimationChannel channel) {
        this.channel = channel;
    }

    @Override
    public void interpolate(int indexFrom, int indexTo, float alpha, float[] result) {
        float ax = channel.content.values[indexFrom][0];
        float ay = channel.content.values[indexFrom][1];
        float az = channel.content.values[indexFrom][2];
        float aw = channel.content.values[indexFrom][3];
        float bx = channel.content.values[indexTo][0];
        float by = channel.content.values[indexTo][1];
        float bz = channel.content.values[indexTo][2];
        float bw = channel.content.values[indexTo][3];

        float dot = ax * bx + ay * by + az * bz + aw * bw;
        if (dot < 0) {
            bx = -bx;
            by = -by;
            bz = -bz;
            bw = -bw;
            dot = -dot;
        }
        float epsilon = 1e-6f;
        float s0, s1;
        if ((1.0 - dot) > epsilon) {
            float omega = (float) Math.acos(dot);
            float invSinOmega = 1.0f / (float) Math.sin(omega);
            s0 = (float) Math.sin((1.0 - alpha) * omega) * invSinOmega;
            s1 = (float) Math.sin(alpha * omega) * invSinOmega;
        } else {
            s0 = 1.0f - alpha;
            s1 = alpha;
        }
        float rx = s0 * ax + s1 * bx;
        float ry = s0 * ay + s1 * by;
        float rz = s0 * az + s1 * bz;
        float rw = s0 * aw + s1 * bw;
        result[0] = rx;
        result[1] = ry;
        result[2] = rz;
        result[3] = rw;
    }
}
