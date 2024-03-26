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
import com.tac.guns.api.item.IAttachment;
import com.tac.guns.api.item.IGun;
import com.tac.guns.client.model.BedrockGunModel;
import com.tac.guns.util.math.MathUtil;
import com.tac.guns.util.math.SecondOrderDynamics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = GunMod.MOD_ID)
public class CameraSetupEvent {
    // 用于平滑 FOV 变化
    private static final SecondOrderDynamics FOV_DYNAMICS = new SecondOrderDynamics(0.5f, 1.2f, 0.5f, 0);
    @SubscribeEvent
    public static void applyLevelCameraAnimation(EntityViewRenderEvent.CameraSetup event) {
        if (!Minecraft.getInstance().options.bobView) {
            return;
        }
        Entity entity = Minecraft.getInstance().getCameraEntity();
        if (entity instanceof LivingEntity livingEntity) {
            // 目前只有枪械物品有摄像机动画
            ItemStack stack = livingEntity.getMainHandItem();
            if (!(stack.getItem() instanceof IGun iGun)) {
                return;
            }
            TimelessAPI.getClientGunIndex(iGun.getGunId(stack)).ifPresent(gunIndex -> {
                BedrockGunModel gunModel = gunIndex.getGunModel();
                Quaternion q = gunModel.getCameraAnimationObject().rotationQuaternion;
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
    }

    @SubscribeEvent
    public static void applyItemInHandCameraAnimation(BeforeRenderHandEvent event) {
        if (!Minecraft.getInstance().options.bobView) {
            return;
        }
        Entity entity = Minecraft.getInstance().getCameraEntity();
        if (entity instanceof LivingEntity livingEntity) {
            // 目前只有枪械物品有摄像机动画
            ItemStack stack = livingEntity.getMainHandItem();
            if (!(stack.getItem() instanceof IGun iGun)) {
                return;
            }
            TimelessAPI.getClientGunIndex(iGun.getGunId(stack)).ifPresent(gunIndex -> {
                BedrockGunModel gunModel = gunIndex.getGunModel();
                PoseStack poseStack = event.getPoseStack();
                poseStack.mulPose(gunModel.getCameraAnimationObject().rotationQuaternion);
            });
        }
    }

    @SubscribeEvent
    public static void applyScopeMagnification(FieldOfView event) {
        if (event.isItemWithHand()) {
            return;
        }
        Entity entity = event.getCamera().getEntity();
        if (entity instanceof LivingEntity livingEntity) {
            ItemStack stack = livingEntity.getMainHandItem();
            if (!(stack.getItem() instanceof IGun iGun)) {
                return;
            }
            ItemStack scopeItem = iGun.getAttachment(stack, AttachmentType.SCOPE);
            if (!(scopeItem.getItem() instanceof IAttachment iAttachment)) {
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
}
