package com.tacz.guns.client.model.listener.constraint;

import com.tacz.guns.api.client.animation.AnimationListener;
import com.tacz.guns.api.client.animation.ObjectAnimationChannel;
import com.tacz.guns.util.math.MathUtil;

public class ConstraintRotateListener implements AnimationListener {
    private final ConstraintObject constraint;

    public ConstraintRotateListener(ConstraintObject constraint) {
        this.constraint = constraint;
    }

    @Override
    public void update(float[] values, boolean blend) {
        if (values.length == 4) {
            values = MathUtil.toEulerAngles(values);
        }
        float x = (float) Math.abs(Math.toDegrees(values[0]));
        float y = (float) Math.abs(Math.toDegrees(values[1]));
        float z = (float) Math.abs(Math.toDegrees(values[2]));
        if (blend) {
            constraint.rotationConstraint.set(
                    Math.max(constraint.rotationConstraint.x(), x),
                    Math.max(constraint.rotationConstraint.y(), y),
                    Math.max(constraint.rotationConstraint.z(), z)
            );
        } else {
            constraint.rotationConstraint.set(x, y, z);
        }
    }

    @Override
    public float[] initialValue() {
        return MathUtil.toQuaternion(constraint.node.xRot, constraint.node.yRot, constraint.node.zRot);
    }

    @Override
    public ObjectAnimationChannel.ChannelType getType() {
        return ObjectAnimationChannel.ChannelType.ROTATION;
    }
}
