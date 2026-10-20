package com.tacz.guns.mixin.client.ar;

import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleInstance;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import com.tacz.guns.client.util.FirstPersonRenderMath;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Preserve the AR mesh path while making its local quad face the view, independently of gun rotation. */
@Mixin(targets = "com.github.mcmodderanchor.simplebedrockmodel.v1.client.compat.acceleratedrendering.AcceleratedParticleRenderer", remap = false)
public abstract class AcceleratedParticleRendererMixin {
    @ModifyArg(method = "renderBillboardAccelerated", at = @At(value = "INVOKE",
            target = "Lcom/github/argon4w/acceleratedrendering/core/buffers/accelerated/builders/IAcceleratedVertexConsumer;beginTransform(Lorg/joml/Matrix4f;Lorg/joml/Matrix3f;)V"), index = 0)
    private Matrix4f tacz$billboardPose(Matrix4f transform, @Local(argsOnly = true) ParticleInstance particle) {
        return FirstPersonRenderMath.billboardPose(RenderSystem.getModelViewMatrix(), transform,
                new Vector3f(particle.x, particle.y, particle.z));
    }
}
