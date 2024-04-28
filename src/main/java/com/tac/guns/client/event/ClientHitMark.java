package com.tac.guns.client.event;

import com.tac.guns.api.TimelessAPI;
import com.tac.guns.api.event.common.LivingHurtByGunEvent;
import com.tac.guns.api.event.common.LivingKillByGunEvent;
import com.tac.guns.client.gui.overlay.KillAmountOverlay;
import com.tac.guns.client.sound.SoundPlayManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ClientHitMark {
    @SubscribeEvent
    public static void onEntityHurt(LivingHurtByGunEvent event) {
        LogicalSide logicalSide = event.getLogicalSide();
        if (logicalSide != LogicalSide.CLIENT) {
            return;
        }
        LivingEntity attacker = event.getAttacker();
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.equals(attacker)) {
            RenderCrosshairEvent.markHitTimestamp();
            if (event.isHeadShot()) {
                RenderCrosshairEvent.markHeadShotTimestamp();
            }
        }
    }

    @SubscribeEvent
    public static void onEntityKill(LivingKillByGunEvent event) {
        LogicalSide logicalSide = event.getLogicalSide();
        if (logicalSide != LogicalSide.CLIENT) {
            return;
        }
        LivingEntity attacker = event.getAttacker();
        ResourceLocation gunId = event.getGunId();
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.equals(attacker)) {
            RenderCrosshairEvent.markKillTimestamp();
            KillAmountOverlay.markTimestamp();
            if (event.isHeadShot()) {
                RenderCrosshairEvent.markHeadShotTimestamp();
                TimelessAPI.getClientGunIndex(gunId).ifPresent(index -> SoundPlayManager.playHeadshotSound(player, index));
            } else {
                TimelessAPI.getClientGunIndex(gunId).ifPresent(index -> SoundPlayManager.playFleshshotSound(player, index));
            }
        }
    }
}
