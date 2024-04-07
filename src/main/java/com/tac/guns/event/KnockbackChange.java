package com.tac.guns.event;

import com.tac.guns.api.entity.IGunOperator;
import com.tac.guns.api.entity.KnockBackModifier;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class KnockbackChange {
    @SubscribeEvent
    public static void onKnockback(LivingKnockBackEvent event) {
        KnockBackModifier modifier = KnockBackModifier.fromLivingEntity(event.getEntityLiving());
        double strength = modifier.getKnockBackStrength();
        if (strength >= 0) {
            event.setStrength((float) strength);
        }
    }
}
