package com.tacz.guns.event;

import com.tacz.guns.GunMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(value = Dist.DEDICATED_SERVER, modid = GunMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CommonLoadPack {
    @SubscribeEvent
    public static void loadGunPack(FMLCommonSetupEvent commonSetupEvent) {
//        DedicatedServerReloadManager.loadGunPack();
    }
}
