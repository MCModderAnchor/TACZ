package com.tacz.guns.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.event.BeforeRenderHandEvent;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.client.other.KeepingItemRenderer;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.event.common.GunFireEvent;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.resource.index.ClientAttachmentIndex;
import com.tacz.guns.client.resource.index.ClientGunIndex;
import com.tacz.guns.resource.pojo.data.attachment.RecoilModifier;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.util.AttachmentDataUtils;
import com.tacz.guns.util.math.MathUtil;
import com.tacz.guns.util.math.SecondOrderDynamics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.joml.Quaternionf;

import java.util.Optional;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = GunMod.MOD_ID)
public class CameraSetupEvent {
    /**
     * 用于平滑 FOV 变化
     */
    private static final SecondOrderDynamics WORLD_FOV_DYNAMICS = new SecondOrderDynamics(0.5f, 1.2f, 0.5f, 0);
    private static final SecondOrderDynamics ITEM_MODEL_FOV_DYNAMICS = new SecondOrderDynamics(0.5f, 1.2f, 0.5f, 0);
    private static PolynomialSplineFunction pitchSplineFunction;
    private static PolynomialSplineFunction yawSplineFunction;
    private static long shootTimeStamp = -1L;
    private static double xRotO = 0;
    private static double yRot0 = 0;
    private static BedrockGunModel lastModel = null;

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
        if (!(stack.getItem() instanceof IGun iGun)) {
            return;
        }
        TimelessAPI.getClientGunIndex(iGun.getGunId(stack)).ifPresent(gunIndex -> {
            BedrockGunModel gunModel = gunIndex.getGunModel();
            if (lastModel != gunModel) {
                // 切换枪械模型的时候清理一下摄像机动画数据，以避免上一次播放到一半的摄像机动画影响观感。
                gunModel.cleanCameraAnimationTransform();
                lastModel = gunModel;
            }
            IClientPlayerGunOperator clientPlayerGunOperator = IClientPlayerGunOperator.fromLocalPlayer(player);
            float partialTicks = Minecraft.getInstance().getFrameTime();
            float aimingProgress = clientPlayerGunOperator.getClientAimingProgress(partialTicks);
            float zoom = iGun.getAimingZoom(stack);
            float multiplier = 1 - aimingProgress + aimingProgress / (float) Math.sqrt(zoom);
            Quaternionf q = MathUtil.multiplyQuaternion(gunModel.getCameraAnimationObject().rotationQuaternion, multiplier);
            double yaw = Math.asin(2 * (q.w() * q.y() - q.x() * q.z()));
            double pitch = Math.atan2(2 * (q.w() * q.x() + q.y() * q.z()), 1 - 2 * (q.x() * q.x() + q.y() * q.y()));
            double roll = Math.atan2(2 * (q.w() * q.z() + q.x() * q.y()), 1 - 2 * (q.y() * q.y() + q.z() * q.z()));
            yaw = Math.toDegrees(yaw);
            pitch = Math.toDegrees(pitch);
            roll = Math.toDegrees(roll);
            event.setYaw((float) yaw + event.getYaw());
            event.setPitch((float) pitch + event.getPitch());
            event.setRoll((float) roll + event.getRoll());
        });
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
        if (!(stack.getItem() instanceof IGun iGun)) {
            return;
        }
        TimelessAPI.getClientGunIndex(iGun.getGunId(stack)).ifPresent(gunIndex -> {
            BedrockGunModel gunModel = gunIndex.getGunModel();
            PoseStack poseStack = event.getPoseStack();
            IClientPlayerGunOperator clientPlayerGunOperator = IClientPlayerGunOperator.fromLocalPlayer(player);
            float partialTicks = Minecraft.getInstance().getFrameTime();
            float aimingProgress = clientPlayerGunOperator.getClientAimingProgress(partialTicks);
            float zoom = iGun.getAimingZoom(stack);
            float multiplier = 1 - aimingProgress + aimingProgress / (float) Math.sqrt(zoom);
            Quaternionf quaternion = MathUtil.multiplyQuaternion(gunModel.getCameraAnimationObject().rotationQuaternion, multiplier);
            poseStack.mulPose(quaternion);
            // 截至目前，摄像机动画数据已消费完毕。是否有更好的清理动画数据的方法？
            gunModel.cleanCameraAnimationTransform();
        });

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
            if (livingEntity instanceof LocalPlayer localPlayer) {
                IClientPlayerGunOperator gunOperator = IClientPlayerGunOperator.fromLocalPlayer(localPlayer);
                float aimingProgress = gunOperator.getClientAimingProgress((float) event.getPartialTick());
                float fov = WORLD_FOV_DYNAMICS.update((float) MathUtil.magnificationToFov(1 + (zoom - 1) * aimingProgress, event.getFOV()));
                event.setFOV(fov);
            } else {
                IGunOperator gunOperator = IGunOperator.fromLivingEntity(livingEntity);
                float aimingProgress = gunOperator.getSynAimingProgress();
                float fov = WORLD_FOV_DYNAMICS.update((float) MathUtil.magnificationToFov(1 + (zoom - 1) * aimingProgress, event.getFOV()));
                event.setFOV(fov);
            }
        }
    }

    @SubscribeEvent
    public static void applyScopeModelFovModifying(ViewportEvent.ComputeFov event) {
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
            ItemStack scopeItem = iGun.getAttachment(stack, AttachmentType.SCOPE);
            if (!(scopeItem.getItem() instanceof IAttachment iAttachment)) {
                float fov = ITEM_MODEL_FOV_DYNAMICS.update((float) event.getFOV());
                event.setFOV(fov);
                return;
            }
            float modifiedFov = TimelessAPI.getClientAttachmentIndex(iAttachment.getAttachmentId(scopeItem)).map(ClientAttachmentIndex::getFov).orElse((float) event.getFOV());
            if (livingEntity instanceof LocalPlayer localPlayer) {
                IClientPlayerGunOperator gunOperator = IClientPlayerGunOperator.fromLocalPlayer(localPlayer);
                float aimingProgress = gunOperator.getClientAimingProgress((float) event.getPartialTick());
                float fov = ITEM_MODEL_FOV_DYNAMICS.update(Mth.lerp(aimingProgress, (float) event.getFOV(), modifiedFov));
                event.setFOV(fov);
            } else {
                IGunOperator gunOperator = IGunOperator.fromLivingEntity(livingEntity);
                float aimingProgress = gunOperator.getSynAimingProgress();
                float fov = ITEM_MODEL_FOV_DYNAMICS.update(Mth.lerp(aimingProgress, (float) event.getFOV(), modifiedFov));
                event.setFOV(fov);
            }
        }
    }

    @SubscribeEvent
    public static void initialCameraRecoil(GunFireEvent event) {
        if (event.getLogicalSide().isClient()) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }
            ItemStack mainhandItem = player.getMainHandItem();
            if (!(mainhandItem.getItem() instanceof IGun iGun)) {
                return;
            }
            ResourceLocation gunId = iGun.getGunId(mainhandItem);
            Optional<ClientGunIndex> gunIndexOptional = TimelessAPI.getClientGunIndex(gunId);
            if (gunIndexOptional.isEmpty()) {
                return;
            }
            ClientGunIndex gunIndex = gunIndexOptional.get();
            GunData gunData = gunIndex.getGunData();
            // 获取所有配件对摄像机后坐力的修改
            final float[] attachmentRecoilModifier = new float[]{0f, 0f};
            AttachmentDataUtils.getAllAttachmentData(mainhandItem, gunData, attachmentData -> {
                RecoilModifier recoilModifier = attachmentData.getRecoilModifier();
                if (recoilModifier == null) {
                    return;
                }
                attachmentRecoilModifier[0] += recoilModifier.getPitch();
                attachmentRecoilModifier[1] += recoilModifier.getYaw();
            });
            IClientPlayerGunOperator clientPlayerGunOperator = IClientPlayerGunOperator.fromLocalPlayer(player);
            float partialTicks = Minecraft.getInstance().getFrameTime();
            float aimingProgress = clientPlayerGunOperator.getClientAimingProgress(partialTicks);
            float zoom = iGun.getAimingZoom(mainhandItem);
            float aimingRecoilModifier = 1 - aimingProgress + aimingProgress / (float) Math.sqrt(zoom);
            pitchSplineFunction = gunData.getRecoil().genPitchSplineFunction(modifierNumber(attachmentRecoilModifier[0]) * aimingRecoilModifier);
            yawSplineFunction = gunData.getRecoil().genYawSplineFunction(modifierNumber(attachmentRecoilModifier[1]) * aimingRecoilModifier);
            shootTimeStamp = System.currentTimeMillis();
            xRotO = 0;
        }
    }

    @SubscribeEvent
    public static void applyCameraRecoil(ViewportEvent.ComputeCameraAngles event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        long timeTotal = System.currentTimeMillis() - shootTimeStamp;
        if (pitchSplineFunction != null && pitchSplineFunction.isValidPoint(timeTotal)) {
            double value = pitchSplineFunction.value(timeTotal);
            player.setXRot(player.getXRot() - (float) (value - xRotO));
            xRotO = value;
        }
        if (yawSplineFunction != null && yawSplineFunction.isValidPoint(timeTotal)) {
            double value = yawSplineFunction.value(timeTotal);
            player.setYRot(player.getYRot() - (float) (value - yRot0));
            yRot0 = value;
        }
    }

    private static float modifierNumber(float modifier) {
        return Math.max(0, 1 + modifier);
    }
}
