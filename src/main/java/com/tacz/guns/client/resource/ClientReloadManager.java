package com.tacz.guns.client.resource;

import com.tacz.guns.GunMod;
import com.tacz.guns.client.tab.CustomTabManager;
import com.tacz.guns.resource.CommonGunPackLoader;
import com.tacz.guns.resource.network.CommonGunPackNetwork;
import org.apache.commons.lang3.time.StopWatch;

import java.util.concurrent.TimeUnit;

public class ClientReloadManager {
    public static void reloadAllPack() {
        StopWatch watch = StopWatch.createStarted();
        {
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
            // 清空客户端的 Network Cache，因为客户端自己不需要这个
            CommonGunPackNetwork.clear();
        }
        watch.stop();
        GunMod.LOGGER.info("Model loading time: {} ms", watch.getTime(TimeUnit.MICROSECONDS) / 1000.0);
    }
}
