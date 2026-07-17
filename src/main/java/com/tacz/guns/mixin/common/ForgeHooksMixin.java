package com.tacz.guns.mixin.common;

import com.tacz.guns.init.ModDamageTypes;
import com.tacz.guns.util.BulletDamageContext;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ForgeHooks.class, remap = false)
public abstract class ForgeHooksMixin {
    @Inject(method = "onLivingAttack", at = @At("HEAD"), cancellable = true)
    private static void tacz$hideBulletShooterOnLivingAttack(LivingEntity entity, DamageSource source, float amount,
                                                              CallbackInfoReturnable<Boolean> cir) {
        if (!source.is(ModDamageTypes.BULLETS_TAG)) {
            return;
        }

        LivingAttackEvent event = new LivingAttackEvent(entity, BulletDamageContext.withoutShooter(source), amount);
        boolean canceled = BulletDamageContext.runWithoutShooter(() -> MinecraftForge.EVENT_BUS.post(event));
        cir.setReturnValue(!canceled);
    }

    @Inject(method = "onLivingHurt", at = @At("HEAD"), cancellable = true)
    private static void tacz$hideBulletShooterOnLivingHurt(LivingEntity entity, DamageSource source, float amount,
                                                            CallbackInfoReturnable<Float> cir) {
        if (!source.is(ModDamageTypes.BULLETS_TAG)) {
            return;
        }

        LivingHurtEvent event = new LivingHurtEvent(entity, BulletDamageContext.withoutShooter(source), amount);
        if (BulletDamageContext.runWithoutShooter(() -> MinecraftForge.EVENT_BUS.post(event))) {
            cir.setReturnValue(0.0F);
            return;
        }

        cir.setReturnValue(event.getAmount());
    }

    @Inject(method = "onLivingDamage", at = @At("HEAD"), cancellable = true)
    private static void tacz$hideBulletShooterOnLivingDamage(LivingEntity entity, DamageSource source, float amount,
                                                              CallbackInfoReturnable<Float> cir) {
        if (!source.is(ModDamageTypes.BULLETS_TAG)) {
            return;
        }

        LivingDamageEvent event = new LivingDamageEvent(entity, BulletDamageContext.withoutShooter(source), amount);
        boolean canceled = BulletDamageContext.runWithoutShooter(() -> MinecraftForge.EVENT_BUS.post(event));
        cir.setReturnValue(canceled ? 0.0F : event.getAmount());
    }
}
