package com.tacz.guns.client.init;
import com.tacz.guns.client.gui.AmmoStorageScreen;
import com.tacz.guns.client.gui.GunSmithTableScreen;
import com.tacz.guns.init.ModContainer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
@EventBusSubscriber(value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class ModContainerScreen {
    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModContainer.GUN_SMITH_TABLE_MENU.get(), GunSmithTableScreen::new);
        event.register(ModContainer.AMMO_STORAGE_MENU.get(), AmmoStorageScreen::new);
    }
}
