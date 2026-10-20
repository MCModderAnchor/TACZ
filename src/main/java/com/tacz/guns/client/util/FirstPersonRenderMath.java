package com.tacz.guns.client.util;

import org.joml.*;

import java.lang.Math;

/** CPU conversions for the hand pose and ModelView matrices split by Minecraft 1.21.1. */
public final class FirstPersonRenderMath {
    private FirstPersonRenderMath() {
    }

    /** Extracts a node's origin in view space without changing either render matrix. */
    public static Vector3f viewPosition(Matrix4fc modelView, Matrix4fc pose) {
        return viewPose(modelView, pose).getTranslation(new Vector3f());
    }

    /** The complete CPU view transform; normal geometry still uses the original pose in its shader. */
    public static Matrix4f viewPose(Matrix4fc modelView, Matrix4fc pose) {
        return new Matrix4f(modelView).mul(pose);
    }

    /** Converts view-space vertices to the space expected before the shader applies ModelView. */
    public static Matrix4f viewToRender(Matrix4fc modelView) {
        return new Matrix4f(modelView).invert();
    }

    /** Matches the muzzle's hand-FOV screen position under the world FOV, preserving its depth. */
    public static Vector3f muzzleOffset(Matrix4fc modelView, Matrix4fc pose, double itemFov, double worldFov) {
        Vector3f position = viewPosition(modelView, pose);
        double itemTan = Math.tan(Math.toRadians(itemFov) / 2);
        double worldTan = Math.tan(Math.toRadians(worldFov) / 2);
        float lateral = itemTan > 1.0e-6 ? (float) (worldTan / itemTan) : 1.0f;
        return position.mul(lateral, lateral, 1.0f);
    }

    /** Camera.rotation() maps view-space offsets to world directions, including camera roll. */
    public static Vector3f worldOffset(Quaternionfc cameraRotation, Vector3fc viewOffset) {
        return cameraRotation.transform(viewOffset, new Vector3f());
    }

    /** Camera-relative world transform for smoke positions and velocity, with FOV applied in view space. */
    public static Matrix4f worldParticlePose(Matrix4fc modelView, Matrix4fc pose,
                                             Quaternionfc cameraRotation, float handToWorldRatio) {
        float lateral = Float.isFinite(handToWorldRatio) && handToWorldRatio > 1.0e-4f
                ? 1.0f / handToWorldRatio : 1.0f;
        return new Matrix4f().rotation(cameraRotation).scale(lateral, lateral, 1.0f).mul(modelView).mul(pose);
    }

    /** AR submits a local quad: give it view-aligned unit axes while retaining the transformed center. */
    public static Matrix4f billboardPose(Matrix4fc modelView, Matrix4fc effectivePose, Vector3fc particlePosition) {
        Vector3f center = effectivePose.transformPosition(particlePosition, new Vector3f());
        Matrix4f billboard = viewToRender(modelView);
        Vector3f localCenter = billboard.transformDirection(particlePosition, new Vector3f());
        return billboard.setTranslation(center.sub(localCenter));
    }
}
