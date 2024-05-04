package com.tacz.guns.mixin.common;

import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ServerMessageSwapItem;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerPlayNetHandlerMixin {
    @Shadow
    public ServerPlayer player;

    @Inject(method = "handlePlayerAction", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;stopUsingItem()V"))
    public void applySwapOffhandDraw(ServerboundPlayerActionPacket packetIn, CallbackInfo ci) {
        player.inventoryMenu.broadcastChanges();
        NetworkHandler.sendToClientPlayer(new ServerMessageSwapItem(), player);
    }
}
