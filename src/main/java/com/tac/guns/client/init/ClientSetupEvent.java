package com.tac.guns.client.init;

import com.tac.guns.GunMod;
import com.tac.guns.client.animation.thrid.ThirdPersonManager;
import com.tac.guns.client.gui.GunHudOverlay;
import com.tac.guns.client.input.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.client.gui.OverlayRegistry;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT, modid = GunMod.MOD_ID)
public class ClientSetupEvent {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // 注册键位
        event.enqueueWork(() -> {
            ClientRegistry.registerKeyBinding(InspectKey.INSPECT_KEY);
            ClientRegistry.registerKeyBinding(ReloadKey.RELOAD_KEY);
            ClientRegistry.registerKeyBinding(ShootKey.SHOOT_KEY);
            ClientRegistry.registerKeyBinding(FireSelectKey.FIRE_SELECT_KEY);
            ClientRegistry.registerKeyBinding(AimKey.AIM_KEY);
            ClientRegistry.registerKeyBinding(RefitKey.REFIT_KEY);
        });

        // 注册 HUD
        event.enqueueWork(() -> {
            OverlayRegistry.registerOverlayTop("TAC HUD Overlay", GunHudOverlay::render);
        });

        // 注册自己的的硬编码第三人称动画
        event.enqueueWork(ThirdPersonManager::registerInner);
    }
}
