package com.tac.guns.event;

import com.tac.guns.GunMod;
import com.tac.guns.resource.CommonGunPackLoader;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(value = Dist.DEDICATED_SERVER, modid = GunMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CommonSetupEvent {
    @SubscribeEvent
    public static void loadGunPack(FMLCommonSetupEvent commonSetupEvent){
        CommonGunPackLoader.init();
        CommonGunPackLoader.reloadAsset();
        CommonGunPackLoader.reloadIndex();
    }
}
