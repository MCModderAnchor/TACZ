package com.tacz.guns.client.model.listener.model;

import com.tacz.guns.api.client.animation.AnimationListener;
import com.tacz.guns.api.client.animation.ObjectAnimationChannel;
import com.tacz.guns.client.model.bedrock.ModelRendererWrapper;

public class ModelScaleListener implements AnimationListener {
    private final ModelRendererWrapper rendererWrapper;

    public ModelScaleListener(ModelRendererWrapper rendererWrapper) {
        this.rendererWrapper = rendererWrapper;
    }

    @Override
    public void update(float[] values, boolean blend) {
        if (blend) {
            rendererWrapper.setScaleX(rendererWrapper.getScaleX() * values[0]);
            rendererWrapper.setScaleY(rendererWrapper.getScaleY() * values[1]);
            rendererWrapper.setScaleZ(rendererWrapper.getScaleZ() * values[2]);
        } else {
            rendererWrapper.setScaleX(values[0]);
            rendererWrapper.setScaleY(values[1]);
            rendererWrapper.setScaleZ(values[2]);
        }
    }

    @Override
    public float[] initialValue() {
        return new float[]{1f, 1f, 1f};
    }

    @Override
    public ObjectAnimationChannel.ChannelType getType() {
        return ObjectAnimationChannel.ChannelType.SCALE;
    }
}
