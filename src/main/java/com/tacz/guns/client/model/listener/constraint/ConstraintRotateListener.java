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
        if (blend) {
            constraint.rotationConstraint.set(
                    (float) Math.max(constraint.rotationConstraint.x(), MathUtil.toDegreePositive(values[0])),
                    (float) Math.max(constraint.rotationConstraint.y(), MathUtil.toDegreePositive(values[1])),
                    (float) Math.max(constraint.rotationConstraint.z(), MathUtil.toDegreePositive(values[2]))
            );
        } else {
            constraint.rotationConstraint.set(
                    (float) MathUtil.toDegreePositive(values[0]),
                    (float) MathUtil.toDegreePositive(values[1]),
                    (float) MathUtil.toDegreePositive(values[2]));
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
