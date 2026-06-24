package com.tacz.guns.client.init;

import com.tacz.guns.client.gui.GunSmithTableScreen;
import com.tacz.guns.client.gui.AmmoStorageScreen;
import com.tacz.guns.init.ModContainer;
import com.tacz.guns.inventory.AmmoStorageMenu;
import com.tacz.guns.inventory.GunSmithTableMenu;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModContainerScreen {
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent evt) {
        evt.enqueueWork(() -> {
            MenuScreens.register(GunSmithTableMenu.TYPE, GunSmithTableScreen::new);
            MenuScreens.register(ModContainer.AMMO_STORAGE_MENU.get(), AmmoStorageScreen::new);
        });
    }
}
