package com.tacz.guns.client.model.listener.model;

import com.mojang.math.Quaternion;
import com.tacz.guns.client.animation.AnimationListener;
import com.tacz.guns.client.animation.ObjectAnimationChannel;
import com.tacz.guns.client.model.bedrock.ModelRendererWrapper;
import com.tacz.guns.util.math.MathUtil;

public class ModelRotateListener implements AnimationListener {
    private final ModelRendererWrapper rendererWrapper;

    public ModelRotateListener(ModelRendererWrapper rendererWrapper) {
        this.rendererWrapper = rendererWrapper;
    }

    @Override
    public void update(float[] values, boolean blend) {
        float[] angles = MathUtil.toEulerAngles(values);
        // 计算 roll（绕 x 轴的旋转角）
        float roll = angles[0];
        // 计算 pitch（绕 y 轴的旋转角）
        float pitch = angles[1];
        // 计算 yaw（绕 z 轴的旋转角）
        float yaw = angles[2];
        // 因为模型是上下颠倒的，因此此处 yaw 轴的旋转需要进行取反
        // 要减去模型组的初始旋转值，写入相对值。
        if (blend) {
            // 约束组动画是特殊值，不参与混合
            float[] q = MathUtil.toQuaternion(
                    -roll - rendererWrapper.getRotateAngleX(),
                    -pitch - rendererWrapper.getRotateAngleY(),
                    yaw - rendererWrapper.getRotateAngleZ()
            );
            Quaternion quaternion = MathUtil.toQuaternion(q);
            MathUtil.blendQuaternion(rendererWrapper.getAdditionalQuaternion(), quaternion);
        } else {
            MathUtil.toQuaternion(
                    -roll - rendererWrapper.getRotateAngleX(),
                    -pitch - rendererWrapper.getRotateAngleY(),
                    yaw - rendererWrapper.getRotateAngleZ(),
                    rendererWrapper.getAdditionalQuaternion()
            );
        }
    }

    @Override
    public float[] recover() {
        // 目标是让相对旋转为 0
        return MathUtil.toQuaternion(-rendererWrapper.getRotateAngleX(), rendererWrapper.getRotateAngleY(), rendererWrapper.getRotateAngleZ());
    }

    @Override
    public ObjectAnimationChannel.ChannelType getType() {
        return ObjectAnimationChannel.ChannelType.ROTATION;
    }
}
