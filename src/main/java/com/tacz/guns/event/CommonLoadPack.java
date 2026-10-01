package com.tacz.guns.event;

import com.tacz.guns.GunMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@EventBusSubscriber(value = Dist.DEDICATED_SERVER, modid = GunMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class CommonLoadPack {
    @SubscribeEvent
    public static void loadGunPack(FMLCommonSetupEvent commonSetupEvent) {
//        DedicatedServerReloadManager.loadGunPack();
    }
}
