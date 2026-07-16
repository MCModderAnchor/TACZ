package com.tacz.guns.mixin.common;

import com.tacz.guns.init.ModDamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ForgeHooks.class, remap = false)
public abstract class ForgeHooksMixin {
    @Inject(method = "onLivingHurt", at = @At("HEAD"), cancellable = true)
    private static void tacz$hideBulletShooterOnLivingHurt(LivingEntity entity, DamageSource source, float amount,
                                                            CallbackInfoReturnable<Float> cir) {
        if (!source.is(ModDamageTypes.BULLETS_TAG)) {
            return;
        }

        LivingHurtEvent event = new LivingHurtEvent(entity, tacz$withoutShooter(source), amount);
        if (MinecraftForge.EVENT_BUS.post(event)) {
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

        LivingDamageEvent event = new LivingDamageEvent(entity, tacz$withoutShooter(source), amount);
        cir.setReturnValue(MinecraftForge.EVENT_BUS.post(event) ? 0.0F : event.getAmount());
    }

    @Unique
    private static DamageSource tacz$withoutShooter(DamageSource source) {
        Entity directEntity = source.getDirectEntity();
        if (directEntity == source.getEntity()) {
            directEntity = null;
        }
        return new DamageSource(source.typeHolder(), directEntity, null);
    }
}
