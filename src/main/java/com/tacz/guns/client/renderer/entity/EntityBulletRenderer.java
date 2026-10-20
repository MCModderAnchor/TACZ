package com.tacz.guns.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.client.model.BedrockAmmoModel;
import com.tacz.guns.client.model.bedrock.BedrockModel;
import com.tacz.guns.client.renderer.item.GunItemRendererWrapper;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.InternalAssetLoader;
import com.tacz.guns.client.util.FirstPersonRenderMath;
import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.entity.EntityKineticBullet;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Objects;
import java.util.Optional;

public class EntityBulletRenderer extends EntityRenderer<EntityKineticBullet> {
    public EntityBulletRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    public static Optional<BedrockModel> getModel() {
        return InternalAssetLoader.getBedrockModel(InternalAssetLoader.DEFAULT_BULLET_MODEL);
    }

    @Override
    public void render(EntityKineticBullet bullet, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        ResourceLocation gunId = bullet.getGunId();
        ResourceLocation gunDisplayId = bullet.getGunDisplayId();
        Optional<GunDisplayInstance> display = TimelessAPI.getGunDisplay(gunDisplayId, gunId);
        if (display.isEmpty()) {
            return;
        }
        float @Nullable [] tracerColor = bullet.getTracerColorOverride().orElse(display.get().getTracerColor());
        ResourceLocation ammoId = bullet.getAmmoId();
        TimelessAPI.getClientAmmoIndex(ammoId).ifPresent(ammoIndex -> {
            BedrockAmmoModel ammoEntityModel = ammoIndex.getAmmoEntityModel();
            ResourceLocation textureLocation = ammoIndex.getAmmoEntityTextureLocation();
            if (ammoEntityModel != null && textureLocation != null) {
                poseStack.pushPose();
                poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, bullet.yRotO, bullet.getYRot()) - 180.0F));
                poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTicks, bullet.xRotO, bullet.getXRot())));
                poseStack.translate(0, 1.5, 0);
                poseStack.scale(-1, -1, 1);
                ammoEntityModel.render(poseStack, ItemDisplayContext.GROUND, RenderType.entityTranslucentCull(textureLocation), packedLight, OverlayTexture.NO_OVERLAY);
                poseStack.popPose();
            }

            // 曳光弹发光
            if (bullet.isTracerAmmo()) {
                float[] actualTracerColor = Objects.requireNonNullElse(tracerColor, ammoIndex.getTracerColor());
                renderTracerAmmo(bullet, actualTracerColor, partialTicks, poseStack, packedLight);
            }
        });
    }

    public void renderTracerAmmo(EntityKineticBullet bullet, float[] tracerColor, float partialTicks, PoseStack poseStack, int packedLight) {
        getModel().ifPresent(model -> {
            Entity shooter = bullet.getOwner();
            if (shooter == null) {
                return;
            }
            boolean isFirstPerson = this.entityRenderDispatcher.options.getCameraType().isFirstPerson() && shooter instanceof LocalPlayer;
            if (isFirstPerson && !RenderConfig.FIRST_PERSON_BULLET_TRACER_ENABLE.get()) {
                return;
            }
            poseStack.pushPose();
            {
                float width = 0.005f;
                Vec3 bulletPosition = bullet.getPosition(partialTicks);
                double trailLength = 0.85 * bullet.getDeltaMovement().length();
                double disToEye = bulletPosition.distanceTo(shooter.getEyePosition(partialTicks));
                trailLength = Math.min(trailLength, disToEye * 0.8);

                if (isFirstPerson) {
                    // 第一人称渲染自己的曳光弹的时候需要应用偏移
                    Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
                    Vector3f offset = bullet.getFirstPersonRenderOffset();
                    if (offset == null) {
                        offset = new Vector3f(GunItemRendererWrapper.muzzleRenderOffset);
                        bullet.setCameraXRot(camera.getXRot());
                        bullet.setCameraYRot(camera.getYRot());
                        bullet.setFirstPersonCameraRotation(camera.rotation());
                        bullet.setFirstPersonRenderOffset(offset);
                    }
                    // 按照生存时间减少曳光弹的偏移，避免渲染位置距离落点太远
                    double offsetReducer = Math.max(0, (50 - disToEye)) / 50;
                    Quaternionf cameraRotation = bullet.getFirstPersonCameraRotation();
                    if (cameraRotation == null) {
                        // 兼容通过原有偏移及欧拉角 API 初始化曳光弹的调用方。
                        cameraRotation = new Quaternionf().rotationY((float) Math.toRadians(-bullet.getCameraYRot() - 180f))
                                .rotateX((float) Math.toRadians(-bullet.getCameraXRot()));
                    }
                    // 完整相机旋转包含 roll；只转换位移，不改变曳光弹自身的姿态。
                    Vector3f worldOffset = FirstPersonRenderMath.worldOffset(cameraRotation, offset);
                    poseStack.translate(worldOffset.x * offsetReducer, worldOffset.y * offsetReducer, worldOffset.z * offsetReducer);
                }
                // 说是 override 其实默认值是 1
                // 所以这里直接乘也没关系
                width *= bullet.getTracerSizeOverride();
                width *= (float) Math.max(1.0, disToEye / 3.5);
                poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, bullet.yRotO, bullet.getYRot()) - 180.0F));
                poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTicks, bullet.xRotO, bullet.getXRot())));
                poseStack.translate(0, isFirstPerson ? 0 : -0.2, trailLength / 2.0);
                poseStack.scale(width, width, (float) trailLength);
                // 距离两格外才渲染，只在前 5 tick 判定
                double bulletDistance = bulletPosition.distanceTo(shooter.getEyePosition());
                if (bullet.tickCount >= 5 || bulletDistance > 2) {
                    RenderType type = RenderType.energySwirl(InternalAssetLoader.DEFAULT_BULLET_TEXTURE, 15, 15);
                    model.render(poseStack, ItemDisplayContext.NONE, type, packedLight, OverlayTexture.NO_OVERLAY,
                            tracerColor[0], tracerColor[1], tracerColor[2], 1);
                }
            }
            poseStack.popPose();
        });
    }

    @Override
    protected int getBlockLightLevel(@NotNull EntityKineticBullet entityBullet, @NotNull BlockPos blockPos) {
        return 15;
    }

    @Override
    public boolean shouldRender(EntityKineticBullet bullet, Frustum camera, double pCamX, double pCamY, double pCamZ) {
        AABB aabb = bullet.getBoundingBoxForCulling().inflate(0.5);
        if (aabb.hasNaN() || aabb.getSize() == 0) {
            aabb = new AABB(bullet.getX() - 2.0, bullet.getY() - 2.0, bullet.getZ() - 2.0, bullet.getX() + 2.0, bullet.getY() + 2.0, bullet.getZ() + 2.0);
        }
        return camera.isVisible(aabb);
    }

    @Override
    public ResourceLocation getTextureLocation(@NotNull EntityKineticBullet entity) {
        return null;
    }
}
