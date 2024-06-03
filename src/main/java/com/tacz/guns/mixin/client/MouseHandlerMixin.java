package com.tacz.guns.mixin.client;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.client.resource.index.ClientAttachmentIndex;
import com.tacz.guns.config.client.ZoomConfig;
import com.tacz.guns.util.math.MathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.Optional;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    @ModifyArg(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"), index = 0)
    public double yawSensitivity(double yaw) {
        return tacz$reduceSensitivity(yaw);
    }

    @ModifyArg(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"), index = 1)
    public double pitchSensitivity(double pitch) {
        return tacz$reduceSensitivity(pitch);
    }

    @Unique
    public double tacz$reduceSensitivity(double sensitivity) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return sensitivity;
        }
        ItemStack mainHandItem = player.getMainHandItem();
        IGun iGun = IGun.getIGunOrNull(mainHandItem);
        if (iGun == null) {
            return sensitivity;
        }
        ItemStack attachment = iGun.getAttachment(mainHandItem, AttachmentType.SCOPE);
        IAttachment iAttachment = IAttachment.getIAttachmentOrNull(attachment);
        if (iAttachment == null) {
            return sensitivity;
        }
        ResourceLocation attachmentId = iAttachment.getAttachmentId(attachment);
        Optional<ClientAttachmentIndex> optional = TimelessAPI.getClientAttachmentIndex(attachmentId);
        if (optional.isPresent()) {
            float[] zoom = optional.get().getZoom();
            if (zoom != null && zoom.length > 0) {
                Minecraft minecraft = Minecraft.getInstance();
                // 缩放倍率
                float zoomLevel = zoom[iAttachment.getZoomNumber(attachment) % zoom.length];
                float progress = IGunOperator.fromLivingEntity(player).getSynAimingProgress();
                // 开镜灵敏度系数
                double sensitivityMultiplier = ZoomConfig.ZOOM_SENSITIVITY_BASE_MULTIPLIER.get();
                // 两种状态下的 fov 计算
                double originalFov = minecraft.options.fov().get();
                double currentFov = MathUtil.magnificationToFov(1 + (zoomLevel - 1) * progress, originalFov);
                // 荧幕距离系数，MC 和 COD 一样使用 MDV 标准，默认为 MDV133（系数为 1.33）
                double coefficient = ZoomConfig.SCREEN_DISTANCE_COEFFICIENT.get();
                double denominator = MathUtil.zoomSensitivityRatio(currentFov, originalFov, coefficient) * sensitivityMultiplier;
                return sensitivity * denominator;
            }
        }
        return sensitivity;
    }
}
