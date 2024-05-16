package com.tacz.guns.client.event;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.event.common.LivingHurtByGunEvent;
import com.tacz.guns.api.event.common.LivingKillByGunEvent;
import com.tacz.guns.client.gui.overlay.KillAmountOverlay;
import com.tacz.guns.client.sound.SoundPlayManager;
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
            ResourceLocation gunId = event.getGunId();
            RenderCrosshairEvent.markHitTimestamp();
            if (event.isHeadShot()) {
                RenderCrosshairEvent.markHeadShotTimestamp();
                TimelessAPI.getClientGunIndex(gunId).ifPresent(index -> SoundPlayManager.playHeadHitSound(player, index));
            } else {
                TimelessAPI.getClientGunIndex(gunId).ifPresent(index -> SoundPlayManager.playFleshHitSound(player, index));
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
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.equals(attacker)) {
            RenderCrosshairEvent.markKillTimestamp();
            KillAmountOverlay.markTimestamp();
            TimelessAPI.getClientGunIndex(event.getGunId()).ifPresent(index -> SoundPlayManager.playKillSound(player, index));
            if (event.isHeadShot()) {
                RenderCrosshairEvent.markHeadShotTimestamp();
            }
        }
    }
}
