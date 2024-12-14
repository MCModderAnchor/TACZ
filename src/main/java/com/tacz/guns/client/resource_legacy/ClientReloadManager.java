package com.tacz.guns.client.resource_legacy;

import com.tacz.guns.client.resource_legacy.download.ClientGunPackDownloadManager;
import com.tacz.guns.config.sync.SyncConfig;
import com.tacz.guns.network.message.ServerMessageSyncGunPack;
import com.tacz.guns.resource.VersionChecker;
import com.tacz.guns.resource.network.DataType;
import com.tacz.guns.resource_legacy.CommonGunPackLoader;
import com.tacz.guns.resource_legacy.network.CommonGunPackNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.io.File;
import java.util.EnumMap;
import java.util.Map;

import static com.tacz.guns.config.ServerConfig.SERVER_CONFIG_SPEC;

@Deprecated
public class ClientReloadManager {
    private static final EnumMap<DataType, Map<ResourceLocation, String>> LOCALE_CACHE = new EnumMap<>(DataType.class);

    public static void reloadAllPack() {
        // 版本检查重置
        VersionChecker.clearCache();
        ClientGunPackLoader.init();
        // 先加载全部资源文件
        CommonGunPackLoader.reloadAsset();
        ClientGunPackLoader.reloadAsset();
        // 再加载定义文件
        // 先加载服务端，让其校验数据
        CommonGunPackLoader.reloadIndex();
        ClientGunPackLoader.reloadIndex();
        // 合成表
//        CommonGunPackLoader.reloadRecipes();
        // 联机 / 非联机情况判断
        Minecraft mc = Minecraft.getInstance();
        // 局域网联机（自己是主机），需要给其他玩家发送自己的同步数据
        // 不需要给自己发
        if (mc.hasSingleplayerServer() && mc.getSingleplayerServer() != null && mc.getSingleplayerServer().isPublished()) {
            CommonGunPackNetwork.syncClientExceptSelf(mc.getSingleplayerServer(), Minecraft.getInstance().player);
            return;
        }
        // 多人游戏，自己是客户端，则需要主动加载服务端缓存数据
        if (!mc.isLocalServer()) {
            if (SERVER_CONFIG_SPEC != null && SERVER_CONFIG_SPEC.isLoaded() && !SyncConfig.CLIENT_GUN_PACK_DOWNLOAD_URLS.get().isEmpty()) {
                ClientGunPackDownloadManager.downloadClientGunPack();
            }
            if (!LOCALE_CACHE.isEmpty()) {
                CommonGunPackNetwork.loadFromCache(LOCALE_CACHE);
            }
        }
    }

    public static void loadClientDownloadGunPack(File file) {
        ClientGunPackLoader.readZipAsset(file);
        if (!LOCALE_CACHE.isEmpty()) {
            CommonGunPackNetwork.loadFromCache(LOCALE_CACHE);
        }
    }

    public static void cacheAll(ServerMessageSyncGunPack message) {
        LOCALE_CACHE.clear();
        LOCALE_CACHE.putAll(message.getCache());
    }
}
