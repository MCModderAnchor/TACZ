package com.tacz.guns.client.event;

import com.github.exopandora.shouldersurfing.api.client.IShoulderSurfingCamera;
import com.github.exopandora.shouldersurfing.api.client.ShoulderSurfing;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.event.BeforeRenderHandEvent;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.client.other.KeepingItemRenderer;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.event.common.GunFireEvent;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.api.item.nbt.AttachmentItemDataAccessor;
import com.tacz.guns.api.modifier.ParameterizedCachePair;
import com.tacz.guns.client.renderer.item.AnimateGeoItemRenderer;
import com.tacz.guns.client.resource.pojo.display.gun.AdsCurve;
import com.tacz.guns.client.resource.pojo.display.gun.AdsFov;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.index.ClientAttachmentVariantIndex;
import com.tacz.guns.client.resource.index.ClientGunIndex;
import com.tacz.guns.compat.shouldersurfing.ShoulderSurfingCompat;
import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.modifier.custom.RecoilModifier;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.util.math.Easing;
import com.tacz.guns.util.math.MathUtil;
import com.tacz.guns.util.math.SecondOrderDynamics;
import com.tacz.guns.util.time.GunTime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import java.util.Optional;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = GunMod.MOD_ID)
public class CameraSetupEvent {
    /**
     * 用于平滑 FOV 变化
     */
    public static final SecondOrderDynamics WORLD_FOV_DYNAMICS = new SecondOrderDynamics(0.5f, 1.2f, 0.5f, 0);
    public static final SecondOrderDynamics ITEM_MODEL_FOV_DYNAMICS = new SecondOrderDynamics(0.5f, 1.2f, 0.5f, 0);
    private static PolynomialSplineFunction pitchSplineFunction;
    private static PolynomialSplineFunction yawSplineFunction;
    private static long shootTimeStamp = -1L;
    private static double xRotO = 0;
    private static double yRotO = 0;

    @SubscribeEvent
    public static void applyLevelCameraAnimation(ViewportEvent.ComputeCameraAngles event) {
        if (!Minecraft.getInstance().options.bobView().get()) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        ItemStack stack = KeepingItemRenderer.getRenderer().getCurrentItem();
        // 尝试调用物品的自定义相机动画
        if (IClientItemExtensions.of(stack.getItem()).getCustomRenderer() instanceof AnimateGeoItemRenderer<?, ?> renderer) {
            renderer.applyLevelCameraAnimation(event, stack, player);
        }

    }

    @SubscribeEvent
    public static void applyItemInHandCameraAnimation(BeforeRenderHandEvent event) {
        if (!Minecraft.getInstance().options.bobView().get()) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        ItemStack stack = KeepingItemRenderer.getRenderer().getCurrentItem();
        // 尝试调用物品的自定义相机动画
        if (IClientItemExtensions.of(stack.getItem()).getCustomRenderer() instanceof AnimateGeoItemRenderer<?, ?> renderer) {
            renderer.applyItemInHandCameraAnimation(event, stack, player);
        }
    }

    @SubscribeEvent
    public static void applyScopeMagnification(ViewportEvent.ComputeFov event) {
        if (!event.usedConfiguredFov()) {
            return; // 只修改世界渲染的 fov，因此如果是手部渲染 fov 事件，则返回
        }
        Entity entity = event.getCamera().getEntity();
        if (entity instanceof LivingEntity livingEntity) {
            ItemStack stack = KeepingItemRenderer.getRenderer().getCurrentItem();
            if (!(stack.getItem() instanceof IGun iGun)) {
                float fov = WORLD_FOV_DYNAMICS.update((float) event.getFOV());
                event.setFOV(fov);
                return;
            }
            float zoom = iGun.getAimingZoom(stack);
            AdsFov.Channel curve = TimelessAPI.getGunDisplay(stack)
                    .map(GunDisplayInstance::getAdsFov).map(AdsFov::getWorld).orElse(null);
            float fovProgress = getFovProgress(livingEntity, (float) event.getPartialTick(), curve);
            float targetFov = (float) MathUtil.magnificationToFov(1 + (zoom - 1) * fovProgress, event.getFOV());
            float smoothedFov = WORLD_FOV_DYNAMICS.update(targetFov);
            event.setFOV(curve == null || curve.isSmooth() ? smoothedFov : targetFov);
        }
    }

    @SubscribeEvent
    public static void applyGunModelFovModifying(ViewportEvent.ComputeFov event) {
        if (event.usedConfiguredFov()) {
            return; // 只修改手部物品的 fov，因此如果是世界渲染 fov 事件，则返回
        }
        Entity entity = event.getCamera().getEntity();
        if (entity instanceof LivingEntity livingEntity) {
            ItemStack stack = KeepingItemRenderer.getRenderer().getCurrentItem();
            if (!(stack.getItem() instanceof IGun iGun)) {
                float fov = ITEM_MODEL_FOV_DYNAMICS.update((float) event.getFOV());
                event.setFOV(fov);
                return;
            }
            ResourceLocation scopeItemId = iGun.getAttachmentId(stack, AttachmentType.SCOPE);
            if (scopeItemId.equals(DefaultAssets.EMPTY_ATTACHMENT_ID)) {
                scopeItemId = iGun.getBuiltInAttachmentId(stack, AttachmentType.SCOPE);
            }
            Optional<GunDisplayInstance> gunDisplay = TimelessAPI.getGunDisplay(stack);
            float modelFov = gunDisplay
                    .map(GunDisplayInstance::getModelFov)
                    .orElse((float) event.getFOV());
            CompoundTag scopeTag = iGun.getAttachmentTag(stack, AttachmentType.SCOPE);
            int zoomNumber = AttachmentItemDataAccessor.getZoomNumberFromTag(scopeTag);
            Optional<ClientAttachmentVariantIndex> scopeVariant = TimelessAPI.getClientAttachmentVariantIndex(iGun.getAttachment(stack, AttachmentType.SCOPE));
            ResourceLocation resolvedScopeItemId = scopeItemId;
            // 常驻状态使用枪械本身 model_fov；开镜状态优先使用配件 fov，若无则使用枪械本身 zoom_model_fov。
            float modifiedFov = scopeVariant
                    .map(index -> {
                        float[] viewsFov = index.getViewsFov();
                        return viewsFov[zoomNumber % viewsFov.length];
                    })
                    .orElseGet(() -> TimelessAPI.getClientAttachmentIndex(resolvedScopeItemId)
                            .map(index -> {
                                float[] viewsFov = index.getViewsFov();
                                return viewsFov[zoomNumber % viewsFov.length];
                            })
                            .orElse(
                                    gunDisplay
                                            .map(GunDisplayInstance::getZoomModelFov)
                                            .orElse(modelFov)
                            ));
            AdsFov.Channel curve = gunDisplay.map(GunDisplayInstance::getAdsFov).map(AdsFov::getModel).orElse(null);
            float fovProgress = getFovProgress(livingEntity, (float) event.getPartialTick(), curve);
            float targetFov = Mth.lerp(fovProgress, modelFov, modifiedFov);
            float smoothedFov = ITEM_MODEL_FOV_DYNAMICS.update(targetFov);
            event.setFOV(curve == null || curve.isSmooth() ? smoothedFov : targetFov);
        }
    }

    private static float getFovProgress(LivingEntity entity, float partialTick, AdsCurve curve) {
        float progress = entity instanceof LocalPlayer localPlayer
                ? IClientPlayerGunOperator.fromLocalPlayer(localPlayer).getClientAimingProgress(partialTick)
                : IGunOperator.fromLivingEntity(entity).getSynAimingProgress();
        if (curve == null) {
            return (float) Easing.easeInExpo(progress);
        }
        return curve.evaluate(progress);
    }

    @SubscribeEvent
    public static void initialCameraRecoil(GunFireEvent event) {
        if (event.getLogicalSide().isClient()) {
            LivingEntity shooter = event.getShooter();
            LocalPlayer player = Minecraft.getInstance().player;
            if (!shooter.equals(player)) {
                return;
            }
            ItemStack mainHandItem = player.getMainHandItem();
            if (!(mainHandItem.getItem() instanceof IGun iGun)) {
                return;
            }
            AttachmentCacheProperty cacheProperty = IGunOperator.fromLivingEntity(player).getCacheProperty();
            if (cacheProperty == null) {
                return;
            }
            ResourceLocation gunId = iGun.getGunId(mainHandItem);
            Optional<ClientGunIndex> gunIndexOptional = TimelessAPI.getClientGunIndex(gunId);
            if (gunIndexOptional.isEmpty()) {
                return;
            }
            ClientGunIndex gunIndex = gunIndexOptional.get();
            GunData gunData = gunIndex.getGunData();
            // 获取所有配件对摄像机后坐力的修改
            ParameterizedCachePair<Float, Float> attachmentRecoilModifier = cacheProperty.getCache(RecoilModifier.ID);
            IClientPlayerGunOperator clientPlayerGunOperator = IClientPlayerGunOperator.fromLocalPlayer(player);
            float partialTicks = Minecraft.getInstance().getFrameTime();
            float aimingProgress = clientPlayerGunOperator.getClientAimingProgress(partialTicks);
            float zoom = iGun.getAimingZoom(mainHandItem);
            float aimingRecoilModifier = 1 - aimingProgress + aimingProgress / (float) Math.min(Math.sqrt(zoom), 1.5);
            // 如果是趴下，那么后坐力按 data 设计减少（默认为降低一半）
            if (!player.isSwimming() && player.getPose() == Pose.SWIMMING) {
                aimingRecoilModifier = aimingRecoilModifier * gunData.getCrawlRecoilMultiplier();
            }
            pitchSplineFunction = gunData.getRecoil().genPitchSplineFunction((float) attachmentRecoilModifier.left().eval(aimingRecoilModifier));
            yawSplineFunction = gunData.getRecoil().genYawSplineFunction((float) attachmentRecoilModifier.right().eval(aimingRecoilModifier));
            shootTimeStamp = GunTime.nowMillis();
            xRotO = 0;
            yRotO = 0;
        }
    }

    @SubscribeEvent
    public static void applyCameraRecoil(ViewportEvent.ComputeCameraAngles event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        long timeTotal = GunTime.nowMillis() - shootTimeStamp;
        if (shootTimeStamp >= 0 && timeTotal >= 0 && pitchSplineFunction != null && pitchSplineFunction.isValidPoint(timeTotal)) {
            double value = pitchSplineFunction.value(timeTotal);
            if (ShoulderSurfingCompat.isInstalled() && ShoulderSurfing.getInstance().isShoulderSurfing()) {
                IShoulderSurfingCamera camera = ShoulderSurfing.getInstance().getCamera();
                camera.setXRot(camera.getXRot() - (float) (value - xRotO));
            } else {
                player.setXRot(player.getXRot() - (float) (value - xRotO));
            }
            xRotO = value;
        }
        if (shootTimeStamp >= 0 && timeTotal >= 0 && yawSplineFunction != null && yawSplineFunction.isValidPoint(timeTotal)) {
            double value = yawSplineFunction.value(timeTotal);
            if (ShoulderSurfingCompat.isInstalled() && ShoulderSurfing.getInstance().isShoulderSurfing()) {
                IShoulderSurfingCamera camera = ShoulderSurfing.getInstance().getCamera();
                camera.setYRot(camera.getYRot() - (float) (value - yRotO));
            } else {
                player.setYRot(player.getYRot() - (float) (value - yRotO));
            }
            yRotO = value;
        }
    }

    public static void resetTimestamp() {
        shootTimeStamp = -1;
        pitchSplineFunction = null;
        yawSplineFunction = null;
        xRotO = 0;
        yRotO = 0;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onComputeMovementFov(ComputeFovModifierEvent event) {
        if (!RenderConfig.DISABLE_MOVEMENT_ATTRIBUTE_FOV.get()) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        float f = 1.0f;
        if (player.getMainHandItem().getItem() instanceof AbstractGunItem) {
            if (player.getAbilities().flying) {
                f *= 1.1F;
            }
            event.setNewFovModifier(player.isSprinting() ? 1.15f * f : f);
        }
    }
}
