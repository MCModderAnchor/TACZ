package com.tacz.guns.client.model.listener.constraint;

import com.tacz.guns.api.client.animation.AnimationListener;
import com.tacz.guns.api.client.animation.ObjectAnimationChannel;

public class ConstraintTranslateListener implements AnimationListener {
    private final ConstraintObject constraint;

    public ConstraintTranslateListener(ConstraintObject constraint) {
        this.constraint = constraint;
    }

    @Override
    public void update(float[] values, boolean blend) {
        float x = Math.abs(values[0] * 16);
        float y = Math.abs(values[1] * 16);
        float z = Math.abs(values[2] * 16);
        if (blend) {
            constraint.translationConstraint.set(
                    Math.max(constraint.translationConstraint.x(), x),
                    Math.max(constraint.translationConstraint.y(), y),
                    Math.max(constraint.translationConstraint.z(), z)
            );
        } else {
            constraint.translationConstraint.set(x, y, z);
        }
    }

    @Override
    public float[] initialValue() {
        float[] recover = new float[3];
        if (constraint.bonesItem != null) {
            recover[0] = constraint.bonesItem.getPivot().get(0) / 16f;
            recover[1] = -constraint.bonesItem.getPivot().get(1) / 16f;
            recover[2] = constraint.bonesItem.getPivot().get(2) / 16f;
        } else {
            recover[0] = constraint.node.x / 16f;
            recover[1] = constraint.node.y / 16f;
            recover[2] = constraint.node.z / 16f;
        }
        return recover;
    }

    @Override
    public ObjectAnimationChannel.ChannelType getType() {
        return ObjectAnimationChannel.ChannelType.TRANSLATION;
    }
}
