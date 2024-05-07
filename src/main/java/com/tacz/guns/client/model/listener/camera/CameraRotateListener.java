package com.tacz.guns.client.model.listener.camera;

import com.mojang.math.Quaternion;
import com.tacz.guns.client.animation.AnimationListener;
import com.tacz.guns.client.animation.ObjectAnimationChannel;
import com.tacz.guns.util.math.MathUtil;

public class CameraRotateListener implements AnimationListener {
    private final CameraAnimationObject camera;

    public CameraRotateListener(CameraAnimationObject camera) {
        this.camera = camera;
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
        // 在关键帧中储存的旋转数值并不是摄像头的旋转数值，是世界箱体的旋转数值
        // 最终需要存入rotationQuaternion的是摄像机的旋转（即世界箱体旋转的反相）
        if (blend) {
            float[] q = MathUtil.toQuaternion(-roll, -pitch, -yaw);
            Quaternion quaternion = MathUtil.toQuaternion(q);
            MathUtil.blendQuaternion(camera.rotationQuaternion, quaternion);
        } else {
            MathUtil.toQuaternion(-roll, -pitch, -yaw, camera.rotationQuaternion);
        }
    }

    @Override
    public float[] recover() {
        return MathUtil.toQuaternion(-camera.cameraRenderer.getRotateAngleX(), camera.cameraRenderer.getRotateAngleY(), camera.cameraRenderer.getRotateAngleZ());
    }

    @Override
    public ObjectAnimationChannel.ChannelType getType() {
        return ObjectAnimationChannel.ChannelType.ROTATION;
    }
}
