package com.tacz.guns.mixin.common;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mrcrayfish.controllable.client.binding.context.BindingContext;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.compat.controllable.ControllableInner;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(value = BindingContext.class, remap = false)
public abstract class BindingContextMixin {
    @ModifyReturnValue(method = "isActive", at = @At("RETURN"))
    private boolean controllableIsStupid(boolean original) {
        var instance = BindingContext.class.cast(this);
        if (!(instance instanceof ControllableInner.GunKeyConflict)) return original;
        var player = Minecraft.getInstance().player;
        if (player == null) return original;
        return original && IGun.mainHandHoldGun(player);
    }
}
