package com.tacz.guns.client.model.listener.model;

import com.tacz.guns.api.client.animation.AnimationListener;
import com.tacz.guns.api.client.animation.ObjectAnimationChannel;
import com.tacz.guns.client.model.BedrockGunModel;

public class ModelAdditionalMagazineListener implements AnimationListener {
    private final AnimationListener listener;
    private final BedrockGunModel model;

    public ModelAdditionalMagazineListener(AnimationListener listener, BedrockGunModel model) {
        this.listener = listener;
        this.model = model;
    }

    @Override
    public void update(float[] values, boolean blend) {
        listener.update(values, blend);
        if (model.getAdditionalMagazineNode() != null) {
            model.getAdditionalMagazineNode().visible = true;
        }
    }

    @Override
    public float[] initialValue() {
        return listener.initialValue();
    }

    @Override
    public ObjectAnimationChannel.ChannelType getType() {
        return listener.getType();
    }
}
