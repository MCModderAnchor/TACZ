package com.tacz.guns.event;

import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerChangeGameTypeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber
public class PlayerGameModeChangeEvent {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onGameModeChange(ClientPlayerChangeGameTypeEvent event) {
        if (event.isCanceled()) {
            return;
        }
        GameType newGameType = event.getNewGameType();
        if (newGameType == GameType.SPECTATOR) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                IClientPlayerGunOperator.fromLocalPlayer(player).aimReset(false);
            }
        }
    }
}
