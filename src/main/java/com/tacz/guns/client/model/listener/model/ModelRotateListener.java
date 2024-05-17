package com.tacz.guns.client.model.listener.model;

import com.tacz.guns.client.animation.AnimationListener;
import com.tacz.guns.client.animation.ObjectAnimationChannel;
import com.tacz.guns.client.model.bedrock.ModelRendererWrapper;
import com.tacz.guns.util.math.MathUtil;
import org.joml.Quaternionf;

public class ModelRotateListener implements AnimationListener {
    private final ModelRendererWrapper rendererWrapper;

    public ModelRotateListener(ModelRendererWrapper rendererWrapper) {
        this.rendererWrapper = rendererWrapper;
    }

    @Override
    public void update(float[] values, boolean blend) {
        float[] angles = MathUtil.toEulerAngles(values);
        float xRot = angles[0];
        float yRot = angles[1];
        float zRot = angles[2];
        // 因为模型是上下颠倒的，因此此处 zRot 轴的旋转需要进行取反
        // 要减去模型组的初始旋转值，写入相对值。
        if (blend) {
            // 约束组动画是特殊值，不参与混合
            float[] q = MathUtil.toQuaternion(
                    -xRot - rendererWrapper.getRotateAngleX(),
                    -yRot - rendererWrapper.getRotateAngleY(),
                    zRot - rendererWrapper.getRotateAngleZ()
            );
            Quaternionf quaternion = MathUtil.toQuaternion(q);
            MathUtil.blendQuaternion(rendererWrapper.getAdditionalQuaternion(), quaternion);
        } else {
            MathUtil.toQuaternion(
                    -xRot - rendererWrapper.getRotateAngleX(),
                    -yRot - rendererWrapper.getRotateAngleY(),
                    zRot - rendererWrapper.getRotateAngleZ(),
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
