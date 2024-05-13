package com.tacz.guns.resource.network;

import com.google.common.collect.Maps;
import com.tacz.guns.client.resource.ClientGunPackLoader;
import com.tacz.guns.client.resource.ClientReloadManager;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ServerMessageSyncGunPack;
import com.tacz.guns.resource.CommonGunPackLoader;
import com.tacz.guns.resource.loader.AttachmentDataLoader;
import com.tacz.guns.resource.loader.GunDataLoader;
import com.tacz.guns.resource.loader.RecipeLoader;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.EnumMap;
import java.util.Map;

public class CommonGunPackNetwork {
    private static final EnumMap<DataType, Map<ResourceLocation, String>> NETWORK_CACHE = new EnumMap<>(DataType.class);

    public static void clear() {
        NETWORK_CACHE.clear();
    }

    public static void addData(DataType type, ResourceLocation id, String json) {
        NETWORK_CACHE.computeIfAbsent(type, k -> Maps.newHashMap()).put(id, json);
    }

    public static void syncClient(MinecraftServer server) {
        server.getPlayerList().getPlayers().forEach(player -> NetworkHandler.sendToClientPlayer(new ServerMessageSyncGunPack(NETWORK_CACHE), player));
    }

    public static void syncClient(ServerPlayer player) {
        NetworkHandler.sendToClientPlayer(new ServerMessageSyncGunPack(NETWORK_CACHE), player);
    }

    public static void toNetwork(EnumMap<DataType, Map<ResourceLocation, String>> cache, FriendlyByteBuf buf) {
        buf.writeVarInt(cache.size());
        cache.forEach((type, caches) -> {
            buf.writeEnum(type);
            buf.writeVarInt(caches.size());
            caches.forEach((id, data) -> {
                buf.writeResourceLocation(id);
                buf.writeUtf(data);
            });
        });
    }

    public static EnumMap<DataType, Map<ResourceLocation, String>> fromNetwork(FriendlyByteBuf buf) {
        EnumMap<DataType, Map<ResourceLocation, String>> cache = Maps.newEnumMap(DataType.class);
        int typeSize = buf.readVarInt();
        for (int i = 0; i < typeSize; i++) {
            DataType type = buf.readEnum(DataType.class);
            int size = buf.readVarInt();
            for (int j = 0; j < size; j++) {
                ResourceLocation id = buf.readResourceLocation();
                String json = buf.readUtf();
                cache.computeIfAbsent(type, k -> Maps.newHashMap()).put(id, json);
            }
        }
        return cache;
    }

    /**
     * 原则上来说，这个方法应该只允许客户端调用<br>
     * <br>
     * 1) 玩家进服时调用 <br>
     * 2) 服务器使用重载指令时，发送到玩家客户端调用<br>
     * 3) 服务器玩家重载自己客户端资源时调用<br>
     */
    @OnlyIn(Dist.CLIENT)
    public static void loadFromCache(EnumMap<DataType, Map<ResourceLocation, String>> allCache, boolean fromNetwork) {
        // 这个更新是增量式的更新
        // 玩家安装了服务端没有的枪械包，也会显示，但无法使用
        allCache.forEach((type, cache) -> cache.forEach((id, json) -> {
            switch (type) {
                case GUN_DATA -> GunDataLoader.loadFromJsonString(id, json);
                case ATTACHMENT_DATA -> AttachmentDataLoader.loadFromJsonString(id, json);
                case AMMO_INDEX -> CommonGunPackLoader.loadAmmoFromJsonString(id, json);
                case GUN_INDEX -> CommonGunPackLoader.loadGunFromJsonString(id, json);
                case ATTACHMENT_INDEX -> CommonGunPackLoader.loadAttachmentFromJsonString(id, json);
                case RECIPES -> RecipeLoader.loadFromJsonString(id, json);
            }
        }));
        // 如果是接受的网络包，则缓存一下服务端的这个参数，后续玩家重载自己客户端资源包时要读取
        if (fromNetwork) {
            ClientReloadManager.putAllCache(allCache);
        }
        ClientGunPackLoader.reloadIndex();
    }
}
