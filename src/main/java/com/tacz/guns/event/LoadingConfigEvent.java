package com.tacz.guns.event;

import com.tacz.guns.config.util.HeadShotAABBConfigRead;
import com.tacz.guns.config.util.InteractKeyConfigRead;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class LoadingConfigEvent {
    private static final String CONFIG_NAME = "tacz-server.toml";

    /**
     * 客户端和服务端启动时，会触发此事件
     */
    @SubscribeEvent
    public static void onLoadingConfig(ModConfigEvent.Loading event) {
        String fileName = event.getConfig().getFileName();
        if (CONFIG_NAME.equals(fileName)) {
            HeadShotAABBConfigRead.init();
            InteractKeyConfigRead.init();
        }
    }

    /**
     * 玩家进入服务端，或者服务端自动重置配置时，会触发此方法
     */
    @SubscribeEvent
    public static void onReloadingConfig(ModConfigEvent.Reloading event) {
        String fileName = event.getConfig().getFileName();
        if (CONFIG_NAME.equals(fileName)) {
            HeadShotAABBConfigRead.init();
            InteractKeyConfigRead.init();
//            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientGunPackDownloadManager::downloadClientGunPack);
        }
    }
}
