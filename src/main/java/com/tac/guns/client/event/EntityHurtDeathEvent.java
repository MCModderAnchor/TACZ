package com.tac.guns.client.event;

import com.tac.guns.GunMod;
import com.tac.guns.entity.EntityBullet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = GunMod.MOD_ID)
public class EntityHurtDeathEvent {
    @SubscribeEvent
    public static void onHurtEntity(LivingHurtEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        Entity directEntity = event.getSource().getDirectEntity();
        if (player != null && directEntity instanceof EntityBullet bullet && bullet.ownedBy(player)) {
            GameOverlayEvent.markHitTimestamp();
        }
    }

    @SubscribeEvent
    public static void onHurtEntity(LivingDeathEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        Entity directEntity = event.getSource().getDirectEntity();
        if (player != null && directEntity instanceof EntityBullet bullet && bullet.ownedBy(player)) {
            GameOverlayEvent.markKillTimestamp();
        }
    }
}
