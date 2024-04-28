package com.tac.guns.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Quaternion;
import com.tac.guns.GunMod;
import com.tac.guns.api.TimelessAPI;
import com.tac.guns.api.attachment.AttachmentType;
import com.tac.guns.api.client.event.BeforeRenderHandEvent;
import com.tac.guns.api.client.event.FieldOfView;
import com.tac.guns.api.client.player.IClientPlayerGunOperator;
import com.tac.guns.api.entity.IGunOperator;
import com.tac.guns.api.event.common.GunShootEvent;
import com.tac.guns.api.item.IAttachment;
import com.tac.guns.api.item.IGun;
import com.tac.guns.client.model.BedrockGunModel;
import com.tac.guns.client.resource.index.ClientGunIndex;
import com.tac.guns.duck.KeepingItemRenderer;
import com.tac.guns.resource.pojo.data.attachment.RecoilModifier;
import com.tac.guns.resource.pojo.data.gun.GunData;
import com.tac.guns.util.AttachmentDataUtils;
import com.tac.guns.util.math.MathUtil;
import com.tac.guns.util.math.SecondOrderDynamics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import java.util.Optional;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = GunMod.MOD_ID)
public class CameraSetupEvent {
    // 用于平滑 FOV 变化
    private static final SecondOrderDynamics FOV_DYNAMICS = new SecondOrderDynamics(0.5f, 1.2f, 0.5f, 0);
    private static PolynomialSplineFunction pitchSplineFunction;
    private static PolynomialSplineFunction yawSplineFunction;
    private static long shootTimeStamp = -1L;
    private static double xRotO = 0;
    private static double yRot0 = 0;
    private static BedrockGunModel lastModel = null;

    @SubscribeEvent
    public static void applyLevelCameraAnimation(EntityViewRenderEvent.CameraSetup event) {
        if (!Minecraft.getInstance().options.bobView) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        ItemStack stack = ((KeepingItemRenderer) Minecraft.getInstance().getItemInHandRenderer()).getCurrentItem();
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
            float zoom = IGun.getAimingZoom(stack);
            float multiplier = 1 - aimingProgress + aimingProgress / (float) Math.sqrt(zoom);
            Quaternion q = MathUtil.multiplyQuaternion(gunModel.getCameraAnimationObject().rotationQuaternion, multiplier);
            double yaw = Math.asin(2 * (q.r() * q.j() - q.i() * q.k()));
            double pitch = Math.atan2(2 * (q.r() * q.i() + q.j() * q.k()), 1 - 2 * (q.i() * q.i() + q.j() * q.j()));
            double roll = Math.atan2(2 * (q.r() * q.k() + q.i() * q.j()), 1 - 2 * (q.j() * q.j() + q.k() * q.k()));
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
        if (!Minecraft.getInstance().options.bobView) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        ItemStack stack = ((KeepingItemRenderer) Minecraft.getInstance().getItemInHandRenderer()).getCurrentItem();
        if (!(stack.getItem() instanceof IGun iGun)) {
            return;
        }
        TimelessAPI.getClientGunIndex(iGun.getGunId(stack)).ifPresent(gunIndex -> {
            BedrockGunModel gunModel = gunIndex.getGunModel();
            PoseStack poseStack = event.getPoseStack();
            IClientPlayerGunOperator clientPlayerGunOperator = IClientPlayerGunOperator.fromLocalPlayer(player);
            float partialTicks = Minecraft.getInstance().getFrameTime();
            float aimingProgress = clientPlayerGunOperator.getClientAimingProgress(partialTicks);
            float zoom = IGun.getAimingZoom(stack);
            float multiplier = 1 - aimingProgress + aimingProgress / (float) Math.sqrt(zoom);
            Quaternion quaternion = MathUtil.multiplyQuaternion(gunModel.getCameraAnimationObject().rotationQuaternion, multiplier);
            poseStack.mulPose(quaternion);
            // 截至目前，摄像机动画数据已消费完毕。是否有更好的清理动画数据的方法？
            gunModel.cleanCameraAnimationTransform();
        });

    }

    @SubscribeEvent
    public static void applyScopeMagnification(FieldOfView event) {
        if (event.isItemWithHand()) {
            return;
        }
        Entity entity = event.getCamera().getEntity();
        if (entity instanceof LivingEntity livingEntity) {
            ItemStack stack = ((KeepingItemRenderer) Minecraft.getInstance().getItemInHandRenderer()).getCurrentItem();
            if (!(stack.getItem() instanceof IGun iGun)) {
                float fov = FOV_DYNAMICS.update((float) event.getFOV());
                event.setFOV(fov);
                return;
            }
            ItemStack scopeItem = iGun.getAttachment(stack, AttachmentType.SCOPE);
            if (!(scopeItem.getItem() instanceof IAttachment iAttachment)) {
                float fov = FOV_DYNAMICS.update((float) event.getFOV());
                event.setFOV(fov);
                return;
            }
            TimelessAPI.getClientAttachmentIndex(iAttachment.getAttachmentId(scopeItem)).ifPresent(index -> {
                float[] zoom = index.getZoom();
                if (zoom != null && zoom.length != 0) {
                    float z = zoom[iAttachment.getZoomNumber(scopeItem) % zoom.length];
                    if (livingEntity instanceof LocalPlayer localPlayer) {
                        IClientPlayerGunOperator gunOperator = IClientPlayerGunOperator.fromLocalPlayer(localPlayer);
                        float aimingProgress = gunOperator.getClientAimingProgress((float) event.getPartialTicks());
                        float fov = FOV_DYNAMICS.update((float) MathUtil.magnificationToFov(1 + (z - 1) * aimingProgress, event.getFOV()));
                        event.setFOV(fov);
                    } else {
                        IGunOperator gunOperator = IGunOperator.fromLivingEntity(livingEntity);
                        float aimingProgress = gunOperator.getSynAimingProgress();
                        float fov = FOV_DYNAMICS.update((float) MathUtil.magnificationToFov(1 + (z - 1) * aimingProgress, event.getFOV()));
                        event.setFOV(fov);
                    }
                }
            });
        }
    }

    @SubscribeEvent
    public static void initialCameraRecoil(GunShootEvent event) {
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
            float zoom = IGun.getAimingZoom(mainhandItem);
            float aimingRecoilModifier = 1 - aimingProgress + aimingProgress / (float) Math.sqrt(zoom);
            pitchSplineFunction = gunData.getRecoil().genPitchSplineFunction(modifierNumber(attachmentRecoilModifier[0]) * aimingRecoilModifier);
            yawSplineFunction = gunData.getRecoil().genYawSplineFunction(modifierNumber(attachmentRecoilModifier[1]) * aimingRecoilModifier);
            shootTimeStamp = System.currentTimeMillis();
            xRotO = 0;
        }
    }

    @SubscribeEvent
    public static void applyCameraRecoil(EntityViewRenderEvent.CameraSetup event) {
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
