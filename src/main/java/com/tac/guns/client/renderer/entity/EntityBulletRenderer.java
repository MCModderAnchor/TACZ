package com.tac.guns.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import com.tac.guns.api.TimelessAPI;
import com.tac.guns.client.model.BedrockAmmoModel;
import com.tac.guns.entity.EntityBullet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class EntityBulletRenderer extends EntityRenderer<EntityBullet> {
    public EntityBulletRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public void render(EntityBullet bullet, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        ResourceLocation ammoId = bullet.getAmmoId();
        TimelessAPI.getClientAmmoIndex(ammoId).ifPresent(index -> {
            BedrockAmmoModel ammoEntityModel = index.getAmmoEntityModel();
            ResourceLocation textureLocation = index.getAmmoEntityTextureLocation();
            if (ammoEntityModel != null && textureLocation != null) {
                poseStack.mulPose(Vector3f.YP.rotationDegrees(Mth.lerp(partialTicks, bullet.yRotO, bullet.getYRot()) - 180.0F));
                poseStack.mulPose(Vector3f.XP.rotationDegrees(Mth.lerp(partialTicks, bullet.xRotO, bullet.getXRot())));
                poseStack.pushPose();
                poseStack.translate(0, 1.5, 0);
                poseStack.scale(-1, -1, 1);
                VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentCull(textureLocation));
                ammoEntityModel.render(poseStack, ItemTransforms.TransformType.GROUND, RenderType.entityTranslucentCull(textureLocation), packedLight, OverlayTexture.NO_OVERLAY);
                poseStack.popPose();
            }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(EntityBullet entity) {
        return null;
    }
}
