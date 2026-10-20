package com.tacz.guns.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.event.RenderItemInHandBobEvent;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.client.other.KeepingItemRenderer;
import com.tacz.guns.api.event.common.GunFireEvent;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.nbt.AttachmentItemDataAccessor;
import com.tacz.guns.client.animation.screen.RefitTransform;
import com.tacz.guns.client.model.BedrockAttachmentModel;
import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.tacz.guns.client.model.functional.MuzzleFlashRender;
import com.tacz.guns.client.renderer.item.GunItemRendererWrapper;
import com.tacz.guns.client.resource.index.ClientAttachmentIndex;
import com.tacz.guns.client.resource.index.ClientAttachmentVariantIndex;
import com.tacz.guns.client.resource.index.ClientSlotAdapterIndex;
import com.tacz.guns.client.resource.pojo.display.gun.AdsPath;
import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.util.SlotAdapterHelper;
import com.tacz.guns.util.math.Easing;
import com.tacz.guns.util.math.MathUtil;
import com.tacz.guns.util.math.PerlinNoise;
import com.tacz.guns.util.math.SecondOrderDynamics;
import com.tacz.guns.util.time.GunTime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 负责第一人称的枪械模型额外效果的渲染。其他部分参见 {@link GunItemRendererWrapper}
 */
@EventBusSubscriber(value = Dist.CLIENT, modid = GunMod.MOD_ID)
public class FirstPersonRenderGunEvent {
    private static final SecondOrderDynamics AIMING_DYNAMICS = new SecondOrderDynamics(1.2f, 1.2f, 0.5f, 0);
    private static SecondOrderDynamics SWITCH_VIEW_DYNAMICS;
    // 用于打开改装界面时枪械运动的平滑
    private static final SecondOrderDynamics REFIT_OPENING_DYNAMICS = new SecondOrderDynamics(1f, 1.2f, 0.5f, 0);
    // 用于跳跃延滞动画的平滑
    private static final SecondOrderDynamics JUMPING_DYNAMICS = new SecondOrderDynamics(0.28f, 1f, 0.65f, 0);
    private static final float JUMPING_Y_SWAY = -2f;
    private static final float JUMPING_SWAY_TIME = 0.3f;
    private static final float LANDING_SWAY_TIME = 0.15f;
    // 用于枪械后座的程序动画
    private static final PerlinNoise SHOOT_X_SWAY_NOISE = new PerlinNoise(-0.2f, 0.2f, 400);
    private static final PerlinNoise SHOOT_Y_ROTATION_NOISE = new PerlinNoise(-0.0136f, 0.0136f, 100);
    private static final float SHOOT_Y_SWAY = -0.1f;
    private static final float SHOOT_ANIMATION_TIME = 0.3f;

    private static float jumpingSwayProgress = 0;
    private static boolean lastOnGround = false;
    private static long jumpingTimeStamp = -1;
    private static long shootTimeStamp = -1;
    private static Matrix4f oldAimingViewMatrix;
    private static float oldViewIndex;
    private static int currentViewIndex = -1;

    /**
     * 当主手拿着枪械物品的时候，取消应用在它上面的 viewBobbing，以便应用自定义的跑步/走路动画。
     */
    @SubscribeEvent
    public static void cancelItemInHandViewBobbing(RenderItemInHandBobEvent.BobView event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        ItemStack itemStack = KeepingItemRenderer.getRenderer().getCurrentItem();
        if (IGun.getIGunOrNull(itemStack) != null) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onGunFire(GunFireEvent event) {
        if (event.getLogicalSide().isClient()) {
            LivingEntity shooter = event.getShooter();
            LocalPlayer player = Minecraft.getInstance().player;
            // 第三人称枪口效果：任何射手（本机玩家 + 服务器广播的其他玩家）。
            // 本机玩家的请求仅在其第三人称持枪渲染时被消费（第一人称不可见）。
            com.tacz.guns.client.particle.ThirdPersonMuzzleParticleManager.onShoot(shooter, event.getGunItemStack());
            // 第三人称附着火光：按实体记录开火时间戳（跟随枪口，其他玩家也可见）
            MuzzleFlashRender.onEntityShoot(shooter);
            if (!shooter.equals(player)) {
                return;
            }
            ItemStack mainHandItem = player.getMainHandItem();
            IGun iGun = IGun.getIGunOrNull(mainHandItem);
            if (iGun == null) {
                return;
            }
            TimelessAPI.getClientGunIndex(iGun.getGunId(mainHandItem)).ifPresent(gunIndex -> {
                // 记录开火时间戳，用于后坐力程序动画
                shootTimeStamp = GunTime.nowMillis();
                // 记录枪口火焰数据
                MuzzleFlashRender.onShoot();
                // 记录 renew 枪口粒子数据
                com.tacz.guns.client.particle.MuzzleParticleManager.onShoot(mainHandItem);
            });
        }
    }

    private static boolean bulletFromPlayer(Entity entity) {
        if (entity instanceof EntityKineticBullet entityBullet) {
            return entityBullet.getOwner() instanceof LocalPlayer;
        }
        return false;
    }

    public static void applyFirstPersonGunTransform(LocalPlayer player, ItemStack gunItemStack, PoseStack poseStack, BedrockGunModel model, float partialTicks) {
        // 配合运动曲线，计算改装枪口的打开进度
        float refitScreenOpeningProgress = REFIT_OPENING_DYNAMICS.update(RefitTransform.getOpeningProgress());
        // 配合运动曲线，计算瞄准进度
        float rawAimingProgress = IClientPlayerGunOperator.fromLocalPlayer(player).getClientAimingProgress(partialTicks);
        float aimingProgress = AIMING_DYNAMICS.update(rawAimingProgress);
        AdsPath adsPath = TimelessAPI.getGunDisplay(gunItemStack).map(display -> display.getAdsPath()).orElse(null);
        // 应用枪械动态，如后坐力、持枪跳跃等
        applyGunMovements(model, aimingProgress, partialTicks);
        // 应用各种摄像机定位组的变换（默认持枪、瞄准、改装界面等）
        applyFirstPersonPositioningTransform(poseStack, model, gunItemStack, aimingProgress, refitScreenOpeningProgress, adsPath);
        // 应用动画约束变换
        applyAnimationConstraintTransform(poseStack, model, aimingProgress * (1 - refitScreenOpeningProgress));
    }

    private static void applyGunMovements(BedrockGunModel model, float aimingProgress, float partialTicks) {
        applyShootSwayAndRotation(model, aimingProgress);
        applyJumpingSway(model, partialTicks);
    }

    /**
     * 应用瞄具摄像机定位组、机瞄摄像机定位组和 Idle 摄像机定位组的变换。会在几个摄像机定位之间插值。
     */
    private static void applyFirstPersonPositioningTransform(PoseStack poseStack, BedrockGunModel model, ItemStack stack, float aimingProgress, float refitScreenOpeningProgress, AdsPath adsPath) {
        IGun iGun = IGun.getIGunOrNull(stack);
        if (iGun == null) {
            return;
        }
        Matrix4f transformMatrix = new Matrix4f();
        transformMatrix.identity();
        // 应用瞄准定位
        List<BedrockPart> idleNodePath = model.getIdleSightPath();
        List<BedrockPart> aimingNodePath = null;
        ResourceLocation scopeId = iGun.getAttachmentId(stack, AttachmentType.SCOPE);
        if (scopeId.equals(DefaultAssets.EMPTY_ATTACHMENT_ID)) {
            scopeId = iGun.getBuiltInAttachmentId(stack, AttachmentType.SCOPE);
        }
        Optional<ClientAttachmentIndex> scopeIndexOptional = DefaultAssets.isEmptyAttachmentId(scopeId) ? Optional.empty() : TimelessAPI.getClientAttachmentIndex(scopeId);
        Optional<ClientAttachmentVariantIndex> scopeVariantOptional = TimelessAPI.getClientAttachmentVariantIndex(iGun.getAttachment(com.tacz.guns.client.util.ClientRegistryAccess.registries(), stack, AttachmentType.SCOPE));
        CompoundTag scopeTag = iGun.getAttachmentTag(stack, AttachmentType.SCOPE);
        int zoomNumber = AttachmentItemDataAccessor.getZoomNumberFromTag(scopeTag);
        int viewIndex = 1;
        Vector3f scopeMountOffset = null;
        int mountOffsetInsertIndex = -1;
        if (DefaultAssets.isEmptyAttachmentId(scopeId)) {
            // 未安装瞄具，使用机瞄定位组
            aimingNodePath = model.getIronSightPath();
        } else {
            // 安装瞄具，组合瞄具定位组和瞄具视野定位组
            List<BedrockPart> scopeNodePath = model.getScopePosPath();
            if (scopeNodePath != null) {
                aimingNodePath = new ArrayList<>(scopeNodePath);
                mountOffsetInsertIndex = scopeNodePath.size() - 1;
                scopeMountOffset = getScopeMountOffset(stack, scopeId, scopeVariantOptional);
                if (scopeIndexOptional.isPresent()) {
                    ClientAttachmentIndex scopeIndex = scopeIndexOptional.get();
                    BedrockAttachmentModel attachmentModel = scopeVariantOptional
                            .map(ClientAttachmentVariantIndex::getAttachmentModel)
                            .orElseGet(scopeIndex::getAttachmentModel);
                    int[] views = scopeVariantOptional
                            .map(ClientAttachmentVariantIndex::getViews)
                            .orElseGet(scopeIndex::getViews);
                    viewIndex = views[zoomNumber % views.length] - 1;
                    if (attachmentModel != null) {
                        List<BedrockPart> scopeViewPath = attachmentModel.getScopeViewPath(currentViewIndex == -1 ? viewIndex : currentViewIndex);
                        if (scopeViewPath != null) {
                            aimingNodePath.addAll(scopeViewPath);
                        }
                    }
                }
            }
        }
        Matrix4f aimingViewMatrix = getPositioningNodeInverse(aimingNodePath, scopeMountOffset, mountOffsetInsertIndex);
        // 执行两个 scope view 之间的插值
        if (currentViewIndex == -1) {
            currentViewIndex = viewIndex;
            oldViewIndex = viewIndex;
            oldAimingViewMatrix = aimingViewMatrix;
            SWITCH_VIEW_DYNAMICS = new SecondOrderDynamics(0.35f, 1.2f, 0.3f, viewIndex);
        }
        float view_interpret = SWITCH_VIEW_DYNAMICS.update(viewIndex);
        float span = currentViewIndex - oldViewIndex;
        float switchingProgress = Math.abs(span) < 0.05 ? 1 : (view_interpret - oldViewIndex) / span;
        MathUtil.applyMatrixLerp(aimingViewMatrix, oldAimingViewMatrix, aimingViewMatrix, 1 - switchingProgress);
        if (currentViewIndex != viewIndex) {
            oldAimingViewMatrix = aimingViewMatrix;
            oldViewIndex = view_interpret;
            currentViewIndex = viewIndex;
        }
        // 应用瞄准变换
        float idleScopeHeightAdjust = scopeIndexOptional.map(ClientAttachmentIndex::getIdleScopeHeightAdjust).orElse(0f) / 16.0f;
        Matrix4f idleViewMatrix = getPositioningNodeInverse(idleNodePath);
        MathUtil.applyMatrixLerp(transformMatrix, idleViewMatrix, transformMatrix, (1 - refitScreenOpeningProgress));
        float positioningWeight = 1 - refitScreenOpeningProgress;
        float pathProgress = aimingProgress;
        float rotationProgress = adsPath == null ? aimingProgress : adsPath.rotation(pathProgress);
        MathUtil.applyMatrixLerp(transformMatrix, aimingViewMatrix, transformMatrix, positioningWeight * rotationProgress);
        if (adsPath != null) {
            // 保留原有旋转插值，位移各轴按自己的时间窗口到达同一瞄准终点。
            // 共享已有的平滑开镜进度，避免短 ADS 时间导致模型瞬间横移。
            transformMatrix.m30(transformMatrix.m30() + (aimingViewMatrix.m30() - idleViewMatrix.m30())
                    * (adsPath.x(pathProgress) - rotationProgress) * positioningWeight);
            transformMatrix.m31(transformMatrix.m31() + (aimingViewMatrix.m31() - idleViewMatrix.m31())
                    * (adsPath.y(pathProgress) - rotationProgress) * positioningWeight);
            transformMatrix.m32(transformMatrix.m32() + (aimingViewMatrix.m32() - idleViewMatrix.m32())
                    * (adsPath.z(pathProgress) - rotationProgress) * positioningWeight);
        }
        // 应用改装界面开启时的定位
        float refitTransformProgress = (float) Easing.easeOutCubic(RefitTransform.getTransformProgress());
        AttachmentType oldType = RefitTransform.getOldTransformType();
        AttachmentType currentType = RefitTransform.getCurrentTransformType();
        List<BedrockPart> fromNode = model.getRefitAttachmentViewPath(oldType);
        List<BedrockPart> toNode = model.getRefitAttachmentViewPath(currentType);
        Matrix4f fromViewMatrix = getPositioningNodeInverse(fromNode);
        Matrix4f toViewMatrix = getPositioningNodeInverse(toNode);
        MathUtil.applyMatrixLerp(transformMatrix, fromViewMatrix, transformMatrix, refitScreenOpeningProgress);
        MathUtil.applyMatrixLerp(transformMatrix, toViewMatrix, transformMatrix, refitScreenOpeningProgress * refitTransformProgress);
        float heightProgress = adsPath == null ? aimingProgress : adsPath.y(pathProgress);
        transformMatrix.m31(transformMatrix.m31() + idleScopeHeightAdjust * (1 - heightProgress));
        // 应用变换到 PoseStack
        poseStack.translate(0, 1.5f, 0);
        // 保持 1.20.1 mulPoseMatrix 只更新位置矩阵的语义。
        poseStack.last().pose().mul(transformMatrix);
        poseStack.translate(0, -1.5f, 0);
    }

    /**
     * 获取摄像机定位组的反相矩阵
     */
    @Nonnull
    private static Matrix4f getPositioningNodeInverse(List<BedrockPart> nodePath) {
        return getPositioningNodeInverse(nodePath, null, -1);
    }

    @Nonnull
    private static Matrix4f getPositioningNodeInverse(List<BedrockPart> nodePath, Vector3f mountOffset, int mountOffsetInsertIndex) {
        Matrix4f matrix4f = new Matrix4f();
        matrix4f.identity();
        if (nodePath != null) {
            for (int i = nodePath.size() - 1; i >= 0; i--) {
                if (mountOffset != null && i == mountOffsetInsertIndex) {
                    matrix4f.translate(-mountOffset.x() / 16.0F, mountOffset.y() / 16.0F, -mountOffset.z() / 16.0F);
                }
                BedrockPart part = nodePath.get(i);
                // 计算反向的旋转
                matrix4f.rotate(Axis.XN.rotation(part.xRot));
                matrix4f.rotate(Axis.YN.rotation(part.yRot));
                matrix4f.rotate(Axis.ZN.rotation(part.zRot));
                // 计算反向的位移
                if (part.getParent() != null) {
                    matrix4f.translate(-part.x / 16.0F, -part.y / 16.0F, -part.z / 16.0F);
                } else {
                    matrix4f.translate(-part.x / 16.0F, (1.5F - part.y / 16.0F), -part.z / 16.0F);
                }
            }
        }
        return matrix4f;
    }

    private static Vector3f getScopeMountOffset(ItemStack gunItem, ResourceLocation scopeId,
                                                Optional<ClientAttachmentVariantIndex> scopeVariant) {
        Vector3f combinedOffset = new Vector3f();
        boolean hasOffset = false;

        ResourceLocation adapterId = SlotAdapterHelper.getEffectiveSlotAdapter(gunItem, AttachmentType.SCOPE, scopeId);
        if (adapterId != null) {
            Vector3f adapterOffset = TimelessAPI.getClientSlotAdapterIndex(adapterId)
                    .map(ClientSlotAdapterIndex::getMountOffset)
                    .orElse(null);
            if (adapterOffset != null) {
                combinedOffset.add(adapterOffset);
                hasOffset = true;
            }
        }

        Vector3f variantOffset = scopeVariant.map(ClientAttachmentVariantIndex::getMountOffset).orElse(null);
        if (variantOffset != null) {
            combinedOffset.add(variantOffset);
            hasOffset = true;
        }
        return hasOffset ? combinedOffset : null;
    }

    private static void applyShootSwayAndRotation(BedrockGunModel model, float aimingProgress) {
        BedrockPart rootNode = model.getRootNode();
        if (rootNode != null) {
            float progress = GunTime.isWithin(shootTimeStamp, (long) (SHOOT_ANIMATION_TIME * 1000))
                    ? 1 - (GunTime.nowMillis() - shootTimeStamp) / (SHOOT_ANIMATION_TIME * 1000)
                    : 0;
            progress = (float) Easing.easeOutCubic(progress);
            rootNode.offsetX += SHOOT_X_SWAY_NOISE.getValue() / 16 * progress * (1 - aimingProgress);
            // 基岩版模型 y 轴上下颠倒，sway 值取相反数
            rootNode.offsetY += -SHOOT_Y_SWAY / 16 * progress * (1 - aimingProgress);
            rootNode.additionalQuaternion.mul(Axis.YP.rotation(SHOOT_Y_ROTATION_NOISE.getValue() * progress));
        }
    }

    private static void applyJumpingSway(BedrockGunModel model, float partialTicks) {
        if (jumpingTimeStamp == -1) {
            jumpingTimeStamp = GunTime.nowMillis();
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            double posY = Mth.lerp(partialTicks, Minecraft.getInstance().player.yOld, Minecraft.getInstance().player.getY());
            float velocityY = (float) (posY - Minecraft.getInstance().player.yOld) / partialTicks;
            if (player.onGround()) {
                if (!lastOnGround) {
                    jumpingSwayProgress = velocityY / -0.1f;
                    if (jumpingSwayProgress > 1) {
                        jumpingSwayProgress = 1;
                    }
                    lastOnGround = true;
                } else {
                    jumpingSwayProgress -= (GunTime.nowMillis() - jumpingTimeStamp) / (LANDING_SWAY_TIME * 1000);
                    if (jumpingSwayProgress < 0) {
                        jumpingSwayProgress = 0;
                    }
                }
            } else {
                if (lastOnGround) {
                    // 0.42 是玩家自然起跳的速度
                    jumpingSwayProgress = velocityY / 0.42f;
                    if (jumpingSwayProgress > 1) {
                        jumpingSwayProgress = 1;
                    }
                    lastOnGround = false;
                } else {
                    jumpingSwayProgress -= (GunTime.nowMillis() - jumpingTimeStamp) / (JUMPING_SWAY_TIME * 1000);
                    if (jumpingSwayProgress < 0) {
                        jumpingSwayProgress = 0;
                    }
                }
            }
        }
        jumpingTimeStamp = GunTime.nowMillis();
        float ySway = JUMPING_DYNAMICS.update(JUMPING_Y_SWAY * jumpingSwayProgress);
        BedrockPart rootNode = model.getRootNode();
        if (rootNode != null) {
            // 基岩版模型 y 轴上下颠倒，sway 值取相反数
            rootNode.offsetY += -ySway / 16;
        }
    }

    public static void resetTimestamps() {
        shootTimeStamp = -1;
        jumpingTimeStamp = -1;
        jumpingSwayProgress = 0;
        lastOnGround = false;
    }

    /**
     * 获取动画约束点的变换数据。
     *
     * @param originTranslation   用于输出约束点的原坐标
     * @param animatedTranslation 用于输出约束点经过动画变换之后的坐标
     * @param rotation            用于输出约束点的旋转
     */
    private static void getAnimationConstraintTransform(List<BedrockPart> nodePath, @Nonnull Vector3f originTranslation, @Nonnull Vector3f animatedTranslation, @Nonnull Vector3f rotation) {
        if (nodePath == null) {
            return;
        }
        // 约束点动画变换矩阵
        Matrix4f animeMatrix = new Matrix4f();
        // 约束点初始变换矩阵
        Matrix4f originMatrix = new Matrix4f();
        animeMatrix.identity();
        originMatrix.identity();
        BedrockPart constrainNode = nodePath.get(nodePath.size() - 1);
        for (BedrockPart part : nodePath) {
            // 乘动画位移
            if (part != constrainNode) {
                animeMatrix.translate(part.offsetX, part.offsetY, part.offsetZ);
            }
            // 乘组位移
            if (part.getParent() != null) {
                animeMatrix.translate(part.x / 16.0F, part.y / 16.0F, part.z / 16.0F);
            } else {
                animeMatrix.translate(part.x / 16.0F, (part.y / 16.0F - 1.5F), part.z / 16.0F);
            }
            // 乘动画旋转
            if (part != constrainNode) {
                animeMatrix.rotate(part.additionalQuaternion);
            }
            // 乘组旋转
            animeMatrix.rotate(Axis.ZP.rotation(part.zRot));
            animeMatrix.rotate(Axis.YP.rotation(part.yRot));
            animeMatrix.rotate(Axis.XP.rotation(part.xRot));

            // 乘组位移
            if (part.getParent() != null) {
                originMatrix.translate(part.x / 16.0F, part.y / 16.0F, part.z / 16.0F);
            } else {
                originMatrix.translate(part.x / 16.0F, (part.y / 16.0F - 1.5F), part.z / 16.0F);
            }
            // 乘组旋转
            originMatrix.rotate(Axis.ZP.rotation(part.zRot));
            originMatrix.rotate(Axis.YP.rotation(part.yRot));
            originMatrix.rotate(Axis.XP.rotation(part.xRot));

        }
        // 把变换数据写入输出
        animeMatrix.getTranslation(animatedTranslation);
        originMatrix.getTranslation(originTranslation);
        Vector3f animatedRotation = MathUtil.getEulerAngles(animeMatrix);
        Vector3f originRotation = MathUtil.getEulerAngles(originMatrix);
        animatedRotation.sub(originRotation);
        rotation.set(animatedRotation.x(), animatedRotation.y(), animatedRotation.z());
    }

    /**
     * 应用动画约束变换。
     *
     * @param weight 控制约束变换的权重，用于插值。
     */
    public static void applyAnimationConstraintTransform(PoseStack poseStack, BedrockGunModel gunModel, float weight) {
        List<BedrockPart> nodePath = gunModel.getConstraintPath();
        if (nodePath == null) {
            return;
        }
        if (gunModel.getConstraintObject() == null) {
            return;
        }
        // 获取动画约束点的变换信息
        Vector3f originTranslation = new Vector3f();
        Vector3f animatedTranslation = new Vector3f();
        Vector3f rotation = new Vector3f();
        Vector3f translationICA = gunModel.getConstraintObject().translationConstraint;
        Vector3f rotationICA = gunModel.getConstraintObject().rotationConstraint;
        getAnimationConstraintTransform(nodePath, originTranslation, animatedTranslation, rotation);
        // 配合约束系数，计算约束位移需要的反向位移
        Vector3f inverseTranslation = new Vector3f(originTranslation);
        inverseTranslation.sub(animatedTranslation);
        inverseTranslation.mulDirection(poseStack.last().pose());
        inverseTranslation.mul(translationICA.x() - 1, translationICA.y() - 1, 1 - translationICA.z()); // 基岩版模型的旋转导致 xy 轴要反过来
        // 计算约束旋转需要的反向旋转。因需要插值，获取的是欧拉角
        Vector3f inverseRotation = new Vector3f(rotation);
        inverseRotation.mul(rotationICA.x() - 1, rotationICA.y() - 1, rotationICA.z() - 1);
        // 约束旋转
        poseStack.translate(animatedTranslation.x(), animatedTranslation.y() + 1.5f, animatedTranslation.z());
        poseStack.mulPose(Axis.XP.rotation(inverseRotation.x() * weight));
        poseStack.mulPose(Axis.YP.rotation(inverseRotation.y() * weight));
        poseStack.mulPose(Axis.ZP.rotation(inverseRotation.z() * weight));
        poseStack.translate(-animatedTranslation.x(), -animatedTranslation.y() - 1.5f, -animatedTranslation.z());
        // 约束位移
        Matrix4f poseMatrix = poseStack.last().pose();
        poseMatrix.m30(poseMatrix.m30() - inverseTranslation.x() * weight);
        poseMatrix.m31(poseMatrix.m31() - inverseTranslation.y() * weight);
        poseMatrix.m32(poseMatrix.m32() + inverseTranslation.z() * weight);
    }
}
