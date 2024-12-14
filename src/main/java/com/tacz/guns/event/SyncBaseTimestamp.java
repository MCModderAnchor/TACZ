package com.tacz.guns.event;

import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ServerMessageSyncBaseTimestamp;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber
public class SyncBaseTimestamp {
    @SubscribeEvent
    public static void onPlayerJoinWorld(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player player && !event.getLevel().isClientSide()) {
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), new ServerMessageSyncBaseTimestamp());
        }
    }
}
