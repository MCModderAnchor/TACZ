package com.tacz.guns.event;

import com.tacz.guns.resource.network.CommonGunPackNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public final class EnterServerEvent {
    /**
     * 客户端和服务端都会执行一次
     * 客户端不是冗余的，如果玩家先进服，后进单人，这里会重置成玩家自己的
     */
    @SubscribeEvent
    public static void onLoggedInServer(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            CommonGunPackNetwork.syncClient(serverPlayer);
        }
    }
}
