package com.tacz.guns.client.util;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Keeps axis-specific animation constraints in view space despite the split hand matrices. */
public final class FirstPersonConstraintMath {
    private FirstPersonConstraintMath() {
    }

    /**
     * Returns the additive translation in the pose's pre-ModelView coordinate space.
     * Inputs are read-only, particularly RenderSystem's shared ModelView matrix.
     */
    public static Vector3f translationCorrection(Matrix4fc modelView, Matrix4fc pose,
                                                 Vector3fc difference, Vector3fc constraint, float weight) {
        if (weight == 0.0f) {
            return new Vector3f();
        }
        Matrix4f viewPose = new Matrix4f(modelView).mul(pose);
        // The baseline's XY sign flips and final (-x, -y, +z) writes combine
        // into (1 - constraint) on each view-space axis.
        Vector3f correction = new Vector3f(difference).mulDirection(viewPose)
                .mul(1.0f - constraint.x(), 1.0f - constraint.y(), 1.0f - constraint.z())
                .mul(weight);
        // This is a displacement (w=0), not a position: ModelView translation
        // must not become part of the correction written into the pose.
        return correction.mulDirection(new Matrix4f(modelView).invert());
    }
}
