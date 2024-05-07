package com.tacz.guns.client.model.functional;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.model.IFunctionalRenderer;
import com.tacz.guns.util.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.world.entity.HumanoidArm;

public class LeftHandRender implements IFunctionalRenderer {
    private final BedrockGunModel bedrockGunModel;

    public LeftHandRender(BedrockGunModel bedrockGunModel) {
        this.bedrockGunModel = bedrockGunModel;
    }

    @Override
    public void render(PoseStack poseStack, VertexConsumer vertexBuffer, ItemTransforms.TransformType transformType, int light, int overlay) {
        if (transformType.firstPerson()) {
            if (!bedrockGunModel.getRenderHand()) {
                return;
            }
            poseStack.mulPose(Vector3f.ZP.rotationDegrees(180f));
            Matrix3f normal = poseStack.last().normal().copy();
            Matrix4f pose = poseStack.last().pose().copy();
            //和枪械模型共用顶点缓冲的都需要代理到渲染结束后渲染
            bedrockGunModel.delegateRender((poseStack1, vertexBuffer1, transformType1, light1, overlay1) -> {
                PoseStack poseStack2 = new PoseStack();
                poseStack2.last().normal().mul(normal);
                poseStack2.last().pose().multiply(pose);
                RenderHelper.renderFirstPersonArm(Minecraft.getInstance().player, HumanoidArm.LEFT, poseStack2, light1);
                Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
            });
        }
    }
}
