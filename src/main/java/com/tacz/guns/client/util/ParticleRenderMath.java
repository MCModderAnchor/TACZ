package com.tacz.guns.client.util;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public final class ParticleRenderMath {
    private ParticleRenderMath() {
    }

    /** 1.21.1 entity/item poses are already world-aligned; only restore the camera's world origin. */
    public static Matrix4f worldPose(Matrix4fc cameraRelativePose, double cameraX, double cameraY, double cameraZ) {
        return new Matrix4f(cameraRelativePose)
                .m30((float) (cameraRelativePose.m30() + cameraX))
                .m31((float) (cameraRelativePose.m31() + cameraY))
                .m32((float) (cameraRelativePose.m32() + cameraZ));
    }
}
