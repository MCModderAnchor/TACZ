package com.tacz.guns.client.resource;

import com.google.common.collect.Lists;
import com.tacz.guns.client.tab.CustomTabManager;
import com.tacz.guns.network.message.ServerMessageSyncGunPack;
import com.tacz.guns.resource.CommonGunPackLoader;
import com.tacz.guns.resource.network.CommonGunPackNetwork;
import com.tacz.guns.resource.network.DataType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.io.File;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ClientReloadManager {
    private static final EnumMap<DataType, Map<ResourceLocation, String>> LOCALE_CACHE = new EnumMap<>(DataType.class);
    private static final List<List<String>> LOCALE_DOWNLOAD = Lists.newArrayList();

    public static void reloadAllPack() {
        ClientGunPackLoader.init();
        // 先加载全部资源文件
        CommonGunPackLoader.reloadAsset();
        ClientGunPackLoader.reloadAsset();
        // 再加载定义文件
        // 先加载服务端，让其校验数据
        CommonGunPackLoader.reloadIndex();
        ClientGunPackLoader.reloadIndex();
        // 合成表
        CommonGunPackLoader.reloadRecipes();
        // 创造模式标签页
        CustomTabManager.initAndReload();
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
            if (!LOCALE_DOWNLOAD.isEmpty()) {
                CommonGunPackNetwork.downloadClientGunPack(LOCALE_DOWNLOAD);
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
        LOCALE_DOWNLOAD.clear();
        LOCALE_CACHE.clear();
        LOCALE_DOWNLOAD.addAll(message.getDownload());
        LOCALE_CACHE.putAll(message.getCache());
    }
}
