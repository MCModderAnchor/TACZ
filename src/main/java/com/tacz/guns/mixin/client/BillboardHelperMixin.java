package com.tacz.guns.mixin.client;

import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.render.BillboardHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.client.util.FirstPersonRenderMath;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

/** SBM builds billboard axes in view space, while 1.21.1 splits that space across two matrices. */
@Mixin(value = BillboardHelper.class, remap = false)
public abstract class BillboardHelperMixin {
    @Redirect(method = "renderBillboard", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack$Pose;pose()Lorg/joml/Matrix4f;", remap = true))
    private static Matrix4f tacz$viewPose(PoseStack.Pose pose) {
        return FirstPersonRenderMath.viewPose(RenderSystem.getModelViewMatrix(), pose.pose());
    }

    @ModifyArg(method = "renderBillboard", at = @At(value = "INVOKE",
            target = "Lcom/github/mcmodderanchor/simplebedrockmodel/v1/particle/render/BillboardHelper;vertex(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lorg/joml/Matrix4f;Lorg/joml/Matrix3f;FFFFFLcom/github/mcmodderanchor/simplebedrockmodel/v1/particle/runtime/ParticleInstance;I)V"),
            index = 1, require = 4)
    private static Matrix4f tacz$submissionPose(Matrix4f identity) {
        return FirstPersonRenderMath.viewToRender(RenderSystem.getModelViewMatrix());
    }
}
