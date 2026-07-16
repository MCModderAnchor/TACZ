package com.tacz.guns.mixin.common;

import com.tacz.guns.init.ModAttributes;
import com.tacz.guns.init.ModDamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
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
    private static void tacz$lockBulletHurtAmount(LivingEntity entity, DamageSource source, float amount,
                                                   CallbackInfoReturnable<Float> cir) {
        if (!source.is(ModDamageTypes.BULLETS_TAG)) {
            return;
        }

        LivingHurtEvent event = new LivingHurtEvent(entity, source, amount);
        if (MinecraftForge.EVENT_BUS.post(event)) {
            cir.setReturnValue(0.0F);
            return;
        }

        cir.setReturnValue(tacz$applyBulletResistance(entity, amount));
    }

    @Inject(method = "onLivingDamage", at = @At("HEAD"), cancellable = true)
    private static void tacz$lockBulletDamageAmount(LivingEntity entity, DamageSource source, float amount,
                                                     CallbackInfoReturnable<Float> cir) {
        if (!source.is(ModDamageTypes.BULLETS_TAG)) {
            return;
        }

        LivingDamageEvent event = new LivingDamageEvent(entity, source, amount);
        cir.setReturnValue(MinecraftForge.EVENT_BUS.post(event) ? 0.0F : amount);
    }

    @Unique
    private static float tacz$applyBulletResistance(LivingEntity entity, float amount) {
        AttributeInstance resistance = entity.getAttribute(ModAttributes.BULLET_RESISTANCE.get());
        if (resistance == null) {
            return amount;
        }
        return amount * (float) (1.0D - resistance.getValue());
    }
}
