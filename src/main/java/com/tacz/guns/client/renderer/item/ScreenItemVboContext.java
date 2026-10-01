package com.tacz.guns.client.renderer.item;

import com.tacz.guns.GunMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

/** Tracks the actual screen draw, which occurs after any visible world has been rendered. */
@net.neoforged.fml.common.EventBusSubscriber(value = Dist.CLIENT, modid = GunMod.MOD_ID)
public final class ScreenItemVboContext {
    private static int depth;

    private ScreenItemVboContext() {}

    public static boolean isActive() {
        return depth > 0;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void beforeScreen(ScreenEvent.Render.Pre event) {
        depth++;
    }

    // ForgeHooksClient posts Render.Post even when Render.Pre cancels the screen body.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void afterScreen(ScreenEvent.Render.Post event) {
        if (depth > 0) depth--;
    }
}
