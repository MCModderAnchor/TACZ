package com.tacz.guns.client.model.listener.constraint;

import com.tacz.guns.client.animation.AnimationListener;
import com.tacz.guns.client.animation.ObjectAnimationChannel;

public class ConstraintTranslateListener implements AnimationListener {
    private final ConstraintObject constraint;

    public ConstraintTranslateListener(ConstraintObject constraint) {
        this.constraint = constraint;
    }

    @Override
    public void update(float[] values, boolean blend) {
        if (constraint.bonesItem != null) {
            if (blend) {
                constraint.translationConstraint.set(
                        Math.max(constraint.translationConstraint.x(), -values[0] * 16 - constraint.bonesItem.getPivot().get(0)),
                        Math.max(constraint.translationConstraint.y(), values[1] * 16 - constraint.bonesItem.getPivot().get(1)),
                        Math.max(constraint.translationConstraint.z(), values[2] * 16 - constraint.bonesItem.getPivot().get(2))
                );
            } else {
                constraint.translationConstraint.set(
                        -values[0] * 16 - constraint.bonesItem.getPivot().get(0),
                        values[1] * 16 - constraint.bonesItem.getPivot().get(1),
                        values[2] * 16 - constraint.bonesItem.getPivot().get(2)
                );
            }
        } else {
            if (blend) {
                constraint.translationConstraint.set(
                        Math.max(constraint.translationConstraint.x(), -values[0] * 16 - constraint.node.x),
                        Math.max(constraint.translationConstraint.y(), values[1] * 16 + constraint.node.y),
                        Math.max(constraint.translationConstraint.z(), values[2] * 16 - constraint.node.z)
                );
            } else {
                constraint.translationConstraint.set(
                        -values[0] * 16 - constraint.node.x,
                        values[1] * 16 + constraint.node.y,
                        values[2] * 16 - constraint.node.z
                );
            }
        }
    }

    @Override
    public float[] recover() {
        float[] recover = new float[3];
        if (constraint.bonesItem != null) {
            recover[0] = -constraint.bonesItem.getPivot().get(0) / 16f;
            recover[1] = constraint.bonesItem.getPivot().get(1) / 16f;
            recover[2] = constraint.bonesItem.getPivot().get(2) / 16f;
        } else {
            recover[0] = -constraint.node.x / 16f;
            recover[1] = -constraint.node.y / 16f;
            recover[2] = constraint.node.z / 16f;
        }
        return recover;
    }

    @Override
    public ObjectAnimationChannel.ChannelType getType() {
        return ObjectAnimationChannel.ChannelType.TRANSLATION;
    }
}
