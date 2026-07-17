package com.tacz.guns.mixin.common;

import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.util.BulletDamageContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Projectile.class)
public abstract class ProjectileMixin {
    @Inject(method = "getOwner", at = @At("HEAD"), cancellable = true)
    private void tacz$hideBulletOwnerDuringDamageEvent(CallbackInfoReturnable<Entity> cir) {
        if ((Object) this instanceof EntityKineticBullet && BulletDamageContext.isShooterHidden()) {
            cir.setReturnValue(null);
        }
    }
}
