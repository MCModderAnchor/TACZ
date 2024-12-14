package com.tacz.guns.mixin.common;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.tacz.guns.api.entity.IGunOperator;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {
    @WrapOperation(method = "handlePlayerCommand", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;setSprinting(Z)V"))
    public void cancelSprintCommand(ServerPlayer player, boolean sprint, Operation<Void> original) {
        IGunOperator gunOperator = IGunOperator.fromLivingEntity(player);
        original.call(player, gunOperator.getProcessedSprintStatus(sprint));
    }
}
