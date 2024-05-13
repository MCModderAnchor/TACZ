package com.tacz.guns.client.resource;

import com.tacz.guns.client.tab.CustomTabManager;
import com.tacz.guns.resource.CommonGunPackLoader;
import com.tacz.guns.resource.network.CommonGunPackNetwork;
import com.tacz.guns.resource.network.DataType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.EnumMap;
import java.util.Map;

public class ClientReloadManager {
    private static final EnumMap<DataType, Map<ResourceLocation, String>> LOCALE_CACHE = new EnumMap<>(DataType.class);

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
        if (mc.hasSingleplayerServer() && mc.getSingleplayerServer() != null && mc.getSingleplayerServer().isPublished()) {
            CommonGunPackNetwork.syncClient(mc.getSingleplayerServer());
            return;
        }
        // 多人游戏，自己是客户端，则需要主动加载服务端缓存数据
        if (!mc.isLocalServer() && !LOCALE_CACHE.isEmpty()) {
            CommonGunPackNetwork.loadFromCache(LOCALE_CACHE, false);
        }
    }

    public static void putAllCache(EnumMap<DataType, Map<ResourceLocation, String>> cache) {
        LOCALE_CACHE.clear();
        LOCALE_CACHE.putAll(cache);
    }
}
