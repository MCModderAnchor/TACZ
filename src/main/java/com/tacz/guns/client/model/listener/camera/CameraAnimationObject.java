package com.tacz.guns.client.model.listener.camera;

import com.mojang.math.Quaternion;
import com.tacz.guns.client.animation.AnimationListener;
import com.tacz.guns.client.animation.AnimationListenerSupplier;
import com.tacz.guns.client.animation.ObjectAnimationChannel;
import com.tacz.guns.client.model.bedrock.ModelRendererWrapper;

import static com.tacz.guns.client.model.BedrockAnimatedModel.CAMERA_NODE_NAME;

public class CameraAnimationObject implements AnimationListenerSupplier {
    /**
     * 存在这个四元数中的旋转是世界箱体的旋转，而不是摄像头的旋转（二者互为相反数）
     */
    public Quaternion rotationQuaternion = Quaternion.ONE.copy();

    /**
     * 当相机的节点为根时，cameraRenderer为空
     */
    public ModelRendererWrapper cameraRenderer;

    @Override
    public AnimationListener supplyListeners(String nodeName, ObjectAnimationChannel.ChannelType type) {
        if (!nodeName.equals(CAMERA_NODE_NAME)) {
            return null;
        }
        if (type.equals(ObjectAnimationChannel.ChannelType.ROTATION)) {
            return new CameraRotateListener(this);
        }
        return null;
    }
}

