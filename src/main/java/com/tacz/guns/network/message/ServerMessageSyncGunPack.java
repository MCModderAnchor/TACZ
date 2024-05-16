package com.tacz.guns.network.message;

import com.tacz.guns.client.resource.ClientReloadManager;
import com.tacz.guns.resource.network.CommonGunPackNetwork;
import com.tacz.guns.resource.network.DataType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;


public class ServerMessageSyncGunPack {
    private final EnumMap<DataType, Map<ResourceLocation, String>> cache;

    public ServerMessageSyncGunPack(EnumMap<DataType, Map<ResourceLocation, String>> cache) {
        this.cache = cache;
    }

    public static void encode(ServerMessageSyncGunPack message, FriendlyByteBuf buf) {
        CommonGunPackNetwork.toNetwork(message.cache, buf);
    }

    public static ServerMessageSyncGunPack decode(FriendlyByteBuf buf) {
        return new ServerMessageSyncGunPack(CommonGunPackNetwork.fromNetworkCache(buf));
    }

    public static void handle(ServerMessageSyncGunPack message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> doSync(message));
        }
        context.setPacketHandled(true);
    }


    public EnumMap<DataType, Map<ResourceLocation, String>> getCache() {
        return cache;
    }

    @OnlyIn(Dist.CLIENT)
    private static void doSync(ServerMessageSyncGunPack message) {
        ClientReloadManager.cacheAll(message);
        ClientReloadManager.reloadAllPack();
    }
}
