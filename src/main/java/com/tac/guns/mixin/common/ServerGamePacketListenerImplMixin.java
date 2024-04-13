package com.tac.guns.mixin.common;

import com.tac.guns.api.entity.IGunOperator;
import com.tac.guns.api.gun.ReloadState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {
    @Redirect(method = "handlePlayerCommand", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;setSprinting(Z)V"))
    public void cancelSprintCommand(ServerPlayer player, boolean sprint){
        IGunOperator gunOperator = IGunOperator.fromLivingEntity(player);
        boolean isAiming = gunOperator.getSynIsAiming();
        ReloadState.StateType reloadStateType = gunOperator.getSynReloadState().getStateType();
        if (isAiming || (reloadStateType.isReloading() && !reloadStateType.isReloadFinishing())) {
            player.setSprinting(false);
        } else {
            player.setSprinting(sprint);
        }
    }
}
