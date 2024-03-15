package com.tac.guns.client.event;

import com.tac.guns.GunMod;
import com.tac.guns.client.resource.ClientGunPackLoader;
import com.tac.guns.client.tab.CustomTabManager;
import com.tac.guns.resource.CommonGunPackLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.commons.lang3.time.StopWatch;

import java.util.concurrent.TimeUnit;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ReloadResourceEvent {
    public static final ResourceLocation BLOCK_ATLAS_TEXTURE = new ResourceLocation("textures/atlas/blocks.png");

    @SubscribeEvent
    public static void onTextureStitchEventPost(TextureStitchEvent.Post event) {
        if (BLOCK_ATLAS_TEXTURE.equals(event.getAtlas().location())) {
            reloadAllPack();
        }
    }

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
        }
        watch.stop();
        GunMod.LOGGER.info("Model loading time: {} ms", watch.getTime(TimeUnit.MICROSECONDS) / 1000.0);
    }
}
