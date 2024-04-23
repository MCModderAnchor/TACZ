package com.tac.guns.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import com.tac.guns.api.TimelessAPI;
import com.tac.guns.api.client.player.IClientPlayerGunOperator;
import com.tac.guns.client.model.BedrockAmmoModel;
import com.tac.guns.client.model.bedrock.BedrockModel;
import com.tac.guns.client.renderer.ModRenderType;
import com.tac.guns.client.resource.InternalAssetLoader;
import com.tac.guns.entity.EntityBullet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class EntityBulletRenderer extends EntityRenderer<EntityBullet> {
    public static final ResourceLocation DEFAULT_BULLET_TEXTURE = new ResourceLocation("tac", "textures/entity/basic_bullet.png");
    public static final ResourceLocation DEFAULT_BULLET_MODEL = new ResourceLocation("tac", "models/bedrock/basic_bullet.json");

    public EntityBulletRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    public static Optional<BedrockModel> getModel() {
        return InternalAssetLoader.getBedrockModel(DEFAULT_BULLET_MODEL);
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
                ammoEntityModel.render(poseStack, ItemTransforms.TransformType.GROUND, RenderType.entityTranslucentCull(textureLocation), packedLight, OverlayTexture.NO_OVERLAY);
                poseStack.popPose();
            }else {
                renderDefault(bullet, entityYaw, partialTicks, poseStack, buffer, packedLight);
            }
        });

    }

    public void renderDefault(EntityBullet bullet, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        getModel().ifPresent(model -> {
            if(Minecraft.getInstance().player==null) return;
            Player player = Minecraft.getInstance().player;

            float width = 0.005f;

            poseStack.pushPose();
            {
                double disToEye = bullet.getPosition(partialTicks).distanceTo(player.getEyePosition(partialTicks));
                double trailLength = 0.85 * bullet.getDeltaMovement().length();
                trailLength = Math.min(trailLength, disToEye * 0.8);

                float extraYRot = 0;

                if (this.entityRenderDispatcher.options.getCameraType().isFirstPerson() && bullet.getOwner() instanceof IClientPlayerGunOperator operator && player.is(bullet.getOwner())) {
                    double merge = 8f * trailLength;
                    float s = (float) ((merge - disToEye) / merge);

                    if (disToEye < merge) {
                        // TODO 这里需要计算实际的偏移，这里只是手填的估计值
                        if (operator.getClientAimingProgress(partialTicks) < 0.8) {
                            poseStack.translate(0, -0.1 * (merge - disToEye) / merge, 0);
                        } else {
                            poseStack.translate(0, -0.008 * (merge - disToEye) / merge, 0);
                        }

                        extraYRot = s;
                        trailLength *= s;
                    }

                    trailLength *= 0.75;
                    width *= 1.35f;
                }
                width *= (float) Math.max(1.0, disToEye/3.5);

                poseStack.mulPose(Vector3f.YP.rotationDegrees(Mth.lerp(partialTicks, bullet.yRotO, bullet.getYRot()) - 180.0F + extraYRot));
                poseStack.mulPose(Vector3f.XP.rotationDegrees(Mth.lerp(partialTicks, bullet.xRotO, bullet.getXRot())));

                poseStack.translate(0, 0, trailLength / 2.0);
                poseStack.scale(width, width, (float) trailLength);
                // TODO 这里可以找个地方读入颜色，我不是很知道data放哪好
                model.render(poseStack, ItemTransforms.TransformType.NONE, ModRenderType.entityBullet(DEFAULT_BULLET_TEXTURE), packedLight, OverlayTexture.NO_OVERLAY,
                        1, 0.5f, 0.5f, 1);
            }
            poseStack.popPose();
        });
    }

    @Override
    protected int getBlockLightLevel(@NotNull EntityBullet entityBullet, @NotNull BlockPos blockPos) {
        return 15;
    }

    @Override
    public boolean shouldRender(EntityBullet bullet, Frustum camera, double pCamX, double pCamY, double pCamZ) {
        AABB aabb = bullet.getBoundingBoxForCulling().inflate(0.5);
        if (aabb.hasNaN() || aabb.getSize() == 0) {
            aabb = new AABB(bullet.getX() - 2.0, bullet.getY() - 2.0, bullet.getZ() - 2.0, bullet.getX() + 2.0, bullet.getY() + 2.0, bullet.getZ() + 2.0);
        }
        return camera.isVisible(aabb);
    }

    @Override
    public ResourceLocation getTextureLocation(@NotNull EntityBullet entity) {
        return null;
    }
}
