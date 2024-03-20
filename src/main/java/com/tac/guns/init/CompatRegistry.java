package com.tac.guns.init;

import com.tac.guns.compat.cloth.MenuIntegration;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class CompatRegistry {
    public static final String CLOTH_CONFIG = "cloth_config";

    @SubscribeEvent
    public static void onEnqueue(final InterModEnqueueEvent event) {
        event.enqueueWork(() -> checkModLoad(CLOTH_CONFIG, () -> DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> MenuIntegration::registerModsPage)));
    }

    public static void checkModLoad(String modId, Runnable runnable) {
        if (ModList.get().isLoaded(modId)) {
            runnable.run();
        }
    }
}
