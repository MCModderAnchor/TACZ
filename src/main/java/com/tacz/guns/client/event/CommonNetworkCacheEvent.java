package com.tacz.guns.client.event;

import com.tacz.guns.GunMod;
import com.tacz.guns.client.resource.ClientIndexManager;
import com.tacz.guns.network.message.ServerMessageSyncGunPack;
import com.tacz.guns.resource.CommonAssetsManager;
import com.tacz.guns.resource.network.CommonNetworkCache;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = GunMod.MOD_ID)
public class CommonNetworkCacheEvent {
    @SubscribeEvent
    public static void onClientPlayerLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        ServerMessageSyncGunPack.clearPendingSync();
        if (event.getConnection() == null || event.getConnection().isMemoryConnection()) {
            return;
        }
        CommonAssetsManager.clearInstance();
        CommonNetworkCache.INSTANCE.clear();
        ClientIndexManager.clear();
    }

    @SubscribeEvent
    public static void onClientPlayerLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ServerMessageSyncGunPack.clearPendingSync();
    }
}
