package com.tac.guns.mixin.client;

import com.tac.guns.api.TimelessAPI;
import com.tac.guns.api.attachment.AttachmentType;
import com.tac.guns.api.entity.IGunOperator;
import com.tac.guns.api.item.IAttachment;
import com.tac.guns.api.item.IGun;
import com.tac.guns.client.resource.index.ClientAttachmentIndex;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
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
                float zoomLevel = zoom[iAttachment.getZoomNumber(attachment) % zoom.length];
                float progress = IGunOperator.fromLivingEntity(player).getSynAimingProgress();
                double denominator = 1 + Math.max(zoomLevel - 1, 0) * progress;
                player.turn(yaw / denominator, pitch / denominator);
                return;
            }
        }
        player.turn(yaw, pitch);
    }
}
