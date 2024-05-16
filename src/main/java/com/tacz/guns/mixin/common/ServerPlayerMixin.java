package com.tacz.guns.mixin.common;

import com.tacz.guns.api.entity.IGunOperator;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {
    @Inject(method = "restoreFrom", at = @At("RETURN"))
    public void initialGunOperateData(ServerPlayer pThat, boolean pKeepEverything, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        IGunOperator.fromLivingEntity(player).initialData();
    }
}
