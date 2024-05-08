package com.tacz.guns.client.animation.interpolator;

import com.mojang.math.Quaternion;
import com.tacz.guns.client.animation.AnimationChannelContent;
import com.tacz.guns.client.animation.AnimationChannelContent.LerpMode;
import com.tacz.guns.util.math.MathUtil;

public class CustomInterpolator implements Interpolator {
    private AnimationChannelContent content;

    @Override
    public void compile(AnimationChannelContent content) {
        this.content = content;
    }

    @Override
    public void interpolate(int indexFrom, int indexTo, float alpha, float[] result) {
        LerpMode fromLerpMode = content.lerpModes[indexFrom];
        LerpMode toLerpMode = content.lerpModes[indexTo];
        if (fromLerpMode == LerpMode.SPHERICAL_LINEAR && toLerpMode == LerpMode.SPHERICAL_LINEAR) {
            // 球面线性插值
            this.doSphericalLinearLerp(indexFrom, indexTo, alpha, result);
        } if (fromLerpMode == LerpMode.SPHERICAL_CATMULLROM || toLerpMode == LerpMode.SPHERICAL_CATMULLROM) {
            // 球面 Catmull-Rom 插值
            this.doSphericalCatmullRomLerp(indexFrom, indexTo, alpha, result);
        }else if (fromLerpMode == LerpMode.CATMULLROM || toLerpMode == LerpMode.CATMULLROM) {
            // Catmull-Rom 插值
            this.doCatmullromLerp(indexFrom, indexTo, alpha, result);
        } else {
            // 其他情况的插值计算
            this.doOtherLerp(indexFrom, indexTo, alpha, result);
        }
    }

    private void doOtherLerp(int indexFrom, int indexTo, float alpha, float[] result) {
        // 如果动画值有 6 个，后三个为 Post 数值，用于插值起点
        int offset = content.values[indexFrom].length == 6 ? 3 : 0;
        for (int i = 0; i < 3; i++) {
            if (indexFrom == indexTo) {
                result[i] = content.values[indexFrom][i + offset];
            } else {
                result[i] = content.values[indexFrom][i + offset] * (1 - alpha) + content.values[indexTo][i] * alpha;
            }
        }
    }

    private void doCatmullromLerp(int indexFrom, int indexTo, float alpha, float[] result) {
        float[] vx = new float[4];
        float[] vy = new float[4];
        float[] vz = new float[4];
        int prev = indexFrom == 0 ? 0 : indexFrom - 1;
        int next = indexTo == (content.values.length - 1) ? (content.values.length - 1) : indexTo + 1;
        int prevOffset = content.values[prev].length == 6 ? 3 : 0;
        vx[0] = content.values[prev][prevOffset];
        vy[0] = content.values[prev][1 + prevOffset];
        vz[0] = content.values[prev][2 + prevOffset];
        vx[1] = content.values[indexFrom][0];
        vy[1] = content.values[indexFrom][1];
        vz[1] = content.values[indexFrom][2];
        vx[2] = content.values[indexTo][0];
        vy[2] = content.values[indexTo][1];
        vz[2] = content.values[indexTo][2];
        vx[3] = content.values[next][0];
        vy[3] = content.values[next][1];
        vz[3] = content.values[next][2];
        result[0] = MathUtil.catmullRom(vx, 0.5f, alpha);
        result[1] = MathUtil.catmullRom(vy, 0.5f, alpha);
        result[2] = MathUtil.catmullRom(vz, 0.5f, alpha);
    }

    private void doSphericalLinearLerp(int indexFrom, int indexTo, float alpha, float[] result) {
        // 如果旋转值有 8 个，后四个为 Post 数值，用于插值起点
        int offset = content.values[indexFrom].length == 8 ? 4 : 0;
        float ax = content.values[indexFrom][offset];
        float ay = content.values[indexFrom][1 + offset];
        float az = content.values[indexFrom][2 + offset];
        float aw = content.values[indexFrom][3 + offset];
        float bx = content.values[indexTo][0];
        float by = content.values[indexTo][1];
        float bz = content.values[indexTo][2];
        float bw = content.values[indexTo][3];

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

    private void doSphericalCatmullRomLerp(int indexFrom, int indexTo, float alpha, float[] result) {
        int prev = indexFrom == 0 ? 0 : indexFrom - 1;
        int next = indexTo == (content.values.length - 1) ? (content.values.length - 1) : indexTo + 1;
        int prevOffset = content.values[prev].length == 8 ? 4 : 0;
        float[] prevValue = content.values[prev];
        float[] q0 = new float[]{prevValue[prevOffset], prevValue[1 + prevOffset], prevValue[2 + prevOffset], prevValue[3 + prevOffset]};
        float[] r = MathUtil.catmullRomQuaternion(new float[][]{q0, content.values[indexFrom], content.values[indexTo], content.values[next]}, 0.5f, alpha);
        result[0] = r[0];
        result[1] = r[1];
        result[2] = r[2];
        result[3] = r[3];
    }

    @Override
    public CustomInterpolator clone() {
        try {
            CustomInterpolator interpolator = (CustomInterpolator) super.clone();
            interpolator.content = this.content;
            return interpolator;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
