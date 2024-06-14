package com.tacz.guns.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.client.resource.index.ClientAttachmentIndex;
import com.tacz.guns.client.resource.index.ClientGunIndex;
import com.tacz.guns.config.client.ZoomConfig;
import com.tacz.guns.util.math.MathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @WrapOperation(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    public void reduceSensitivity(LocalPlayer player, double yaw, double pitch, Operation<Void> original) {
        ItemStack mainHandItem = player.getMainHandItem();
        IGun iGun = IGun.getIGunOrNull(mainHandItem);
        if (iGun == null) {
            original.call(player, yaw, pitch);
            return;
        }
        ItemStack attachment = iGun.getAttachment(mainHandItem, AttachmentType.SCOPE);
        IAttachment iAttachment = IAttachment.getIAttachmentOrNull(attachment);
        float zoomLevel = 1;
        if (iAttachment == null) {
            // 缩放倍率
            ResourceLocation gunId = iGun.getGunId(mainHandItem);
            zoomLevel = TimelessAPI.getClientGunIndex(gunId).map(ClientGunIndex::getIronZoom).orElse(1f);
        } else {
            ResourceLocation attachmentId = iAttachment.getAttachmentId(attachment);
            Optional<ClientAttachmentIndex> optional = TimelessAPI.getClientAttachmentIndex(attachmentId);
            if (optional.isPresent()) {
                float[] zoom = optional.get().getZoom();
                if (zoom != null && zoom.length > 0) {
                    zoomLevel = zoom[iAttachment.getZoomNumber(attachment) % zoom.length];
                }
            }
        }
        Minecraft minecraft = Minecraft.getInstance();
        float progress = IGunOperator.fromLivingEntity(player).getSynAimingProgress();
        // 开镜灵敏度系数
        double sensitivityMultiplier = ZoomConfig.ZOOM_SENSITIVITY_BASE_MULTIPLIER.get();
        sensitivityMultiplier = 1 + (sensitivityMultiplier - 1) * progress;
        // 两种状态下的 fov 计算
        double originalFov = minecraft.options.fov().get();
        double currentFov = MathUtil.magnificationToFov(1 + (zoomLevel - 1) * progress, originalFov);
        // 荧幕距离系数，MC 和 COD 一样使用 MDV 标准，默认为 MDV133（系数为 1.33）
        double coefficient = ZoomConfig.SCREEN_DISTANCE_COEFFICIENT.get();
        double denominator = MathUtil.zoomSensitivityRatio(currentFov, originalFov, coefficient) * sensitivityMultiplier;
        original.call(player, yaw * denominator, pitch * denominator);
    }
}
