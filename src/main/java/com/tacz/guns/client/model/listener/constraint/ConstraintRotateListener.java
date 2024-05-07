package com.tacz.guns.client.model.listener.constraint;

import com.tacz.guns.client.animation.AnimationListener;
import com.tacz.guns.client.animation.ObjectAnimationChannel;
import com.tacz.guns.util.math.MathUtil;

public class ConstraintRotateListener implements AnimationListener {
    private final ConstraintObject constraint;

    public ConstraintRotateListener(ConstraintObject constraint) {
        this.constraint = constraint;
    }

    @Override
    public void update(float[] values, boolean blend) {
        float[] angles = MathUtil.toEulerAngles(values);
        if (blend) {
            constraint.rotationConstraint.set(
                    (float) Math.max(constraint.rotationConstraint.x(), MathUtil.toDegreePositive(-angles[0])),
                    (float) Math.max(constraint.rotationConstraint.y(), MathUtil.toDegreePositive(-angles[1])),
                    (float) Math.max(constraint.rotationConstraint.z(), MathUtil.toDegreePositive(angles[2]))
            );
        } else {
            constraint.rotationConstraint.set(
                    (float) MathUtil.toDegreePositive(-angles[0]),
                    (float) MathUtil.toDegreePositive(-angles[1]),
                    (float) MathUtil.toDegreePositive(angles[2]));
        }
    }

    @Override
    public float[] recover() {
        return new float[]{0, 0, 0, 1};
    }

    @Override
    public ObjectAnimationChannel.ChannelType getType() {
        return ObjectAnimationChannel.ChannelType.ROTATION;
    }
}
