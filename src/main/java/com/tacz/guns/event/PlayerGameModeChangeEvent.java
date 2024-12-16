package com.tacz.guns.event;

import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ServerMessagePlayerAimReset;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class PlayerGameModeChangeEvent {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onGameModeChange(PlayerEvent.PlayerChangeGameModeEvent event) {
        if (event.isCanceled()) {
            return;
        }
        GameType newGameType = event.getNewGameMode();
        if (newGameType == GameType.SPECTATOR) {
            Player who = event.getEntity();
            if (!who.level().isClientSide) {
                NetworkHandler.sendToClientPlayer(ServerMessagePlayerAimReset.INSTANCE, who);
            }
        }
    }
}
