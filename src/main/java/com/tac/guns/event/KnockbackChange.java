package com.tac.guns.event;

import com.tac.guns.api.entity.IGunOperator;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class KnockbackChange {
    @SubscribeEvent
    public static void onKnockback(LivingKnockBackEvent event) {
        if (event.getEntityLiving() instanceof IGunOperator operator) {
            double strength = operator.getKnockbackStrength();
            if (strength >= 0) {
                event.setStrength((float) strength);
            }
        }
    }
}
