package com.tacz.guns.mixin.client;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.resource.index.ClientAttachmentIndex;
import com.tacz.guns.config.client.ZoomConfig;
import com.tacz.guns.util.math.MathUtil;
import com.tacz.guns.util.math.SecondOrderDynamics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    // 跟踪 FOV 平滑变化
    @Unique
    private static final SecondOrderDynamics CURRENT_FOV_DYNAMICS = new SecondOrderDynamics(0.5f, 1.2f, 0.5f, 0);
    @Unique
    private static final SecondOrderDynamics ORIGINAL_FOV_DYNAMICS = new SecondOrderDynamics(0.5f, 1.2f, 0.5f, 0);

    @Redirect(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    public void reduceSensitivity(LocalPlayer player, double yaw, double pitch) {
        ItemStack mainHandItem = player.getMainHandItem();
        IGun iGun = IGun.getIGunOrNull(mainHandItem);
        if (iGun == null) {
            player.turn(yaw, pitch);
            return;
        }
        ItemStack attachment = iGun.getAttachment(mainHandItem, AttachmentType.SCOPE);
        IAttachment iAttachment = IAttachment.getIAttachmentOrNull(attachment);
        if (iAttachment == null) {
            player.turn(yaw, pitch);
            return;
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
                double currentFov = CURRENT_FOV_DYNAMICS.update((float) MathUtil.magnificationToFov(1 + (zoomLevel - 1) * progress, minecraft.options.fov));
                double originalFov = ORIGINAL_FOV_DYNAMICS.update((float) minecraft.options.fov);
                // 荧幕距离系数，MC 和 COD 一样使用 MDV 标准，默认为 MDV133（系数为 1.33）
                double coefficient = ZoomConfig.SCREEN_DISTANCE_COEFFICIENT.get();
                double denominator = MathUtil.zoomSensitivityRatio(currentFov, originalFov, coefficient) * sensitivityMultiplier;
                player.turn(yaw * denominator, pitch * denominator);
                return;
            }
        }
        player.turn(yaw, pitch);
    }
}
