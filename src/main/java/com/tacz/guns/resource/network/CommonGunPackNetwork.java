package com.tacz.guns.resource.network;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tacz.guns.client.download.ClientGunPackDownloadManager;
import com.tacz.guns.client.resource.ClientGunPackLoader;
import com.tacz.guns.config.common.OtherConfig;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ServerMessageSyncGunPack;
import com.tacz.guns.resource.loader.asset.AttachmentDataLoader;
import com.tacz.guns.resource.loader.asset.GunDataLoader;
import com.tacz.guns.resource.loader.asset.RecipeLoader;
import com.tacz.guns.resource.loader.index.CommonAmmoIndexLoader;
import com.tacz.guns.resource.loader.index.CommonAttachmentIndexLoader;
import com.tacz.guns.resource.loader.index.CommonGunIndexLoader;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class CommonGunPackNetwork {
    private static final Pattern SHA1 = Pattern.compile("^[a-fA-F0-9]{40}$");
    private static final EnumMap<DataType, Map<ResourceLocation, String>> NETWORK_CACHE = new EnumMap<>(DataType.class);

    public static void clear() {
        NETWORK_CACHE.clear();
    }

    public static void addData(DataType type, ResourceLocation id, String json) {
        NETWORK_CACHE.computeIfAbsent(type, k -> Maps.newHashMap()).put(id, json);
    }

    public static void syncClient(MinecraftServer server) {
        server.getPlayerList().getPlayers().forEach(player -> NetworkHandler.sendToClientPlayer(
                new ServerMessageSyncGunPack(OtherConfig.CLIENT_GUN_PACK_DOWNLOAD_URLS.get(), NETWORK_CACHE), player));
    }

    public static void syncClient(ServerPlayer player) {
        NetworkHandler.sendToClientPlayer(new ServerMessageSyncGunPack(OtherConfig.CLIENT_GUN_PACK_DOWNLOAD_URLS.get(), NETWORK_CACHE), player);
    }

    public static void toNetwork(List<List<String>> download, EnumMap<DataType, Map<ResourceLocation, String>> cache, FriendlyByteBuf buf) {
        buf.writeVarInt(download.size());
        download.forEach(data -> {
            buf.writeUtf(data.get(0));
            buf.writeUtf(data.get(1));
        });

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

    public static List<List<String>> fromNetworkDownload(FriendlyByteBuf buf) {
        List<List<String>> download = Lists.newArrayList();
        int size = buf.readVarInt();
        for (int i = 0; i < size; i++) {
            String url = buf.readUtf();
            String sha1 = buf.readUtf();
            download.add(Lists.newArrayList(url, sha1));
        }
        return download;
    }

    public static EnumMap<DataType, Map<ResourceLocation, String>> fromNetworkCache(FriendlyByteBuf buf) {
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
    public static void loadFromCache(EnumMap<DataType, Map<ResourceLocation, String>> allCache) {
        // 这个更新是增量式的更新
        // 玩家安装了服务端没有的枪械包，也会显示，但无法使用
        allCache.forEach((type, cache) -> cache.forEach((id, json) -> {
            switch (type) {
                case GUN_DATA -> GunDataLoader.loadFromJsonString(id, json);
                case ATTACHMENT_DATA -> AttachmentDataLoader.loadFromJsonString(id, json);
                case AMMO_INDEX -> CommonAmmoIndexLoader.loadAmmoFromJsonString(id, json);
                case GUN_INDEX -> CommonGunIndexLoader.loadGunFromJsonString(id, json);
                case ATTACHMENT_INDEX -> CommonAttachmentIndexLoader.loadAttachmentFromJsonString(id, json);
                case RECIPES -> RecipeLoader.loadFromJsonString(id, json);
            }
        }));
        ClientGunPackLoader.reloadIndex();
    }

    @OnlyIn(Dist.CLIENT)
    public static void downloadClientGunPack(List<List<String>> download) {
        download.forEach(data -> {
            String url = data.get(0);
            String sha1 = data.get(1);
            if (StringUtils.isBlank(url)) {
                return;
            }
            if (!SHA1.matcher(sha1).matches()) {
                return;
            }
            ClientGunPackDownloadManager.download(url, sha1);
        });
    }
}
