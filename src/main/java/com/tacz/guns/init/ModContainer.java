package com.tacz.guns.init;

import com.tacz.guns.GunMod;
import com.tacz.guns.inventory.AmmoStorageMenu;
import com.tacz.guns.inventory.GunSmithTableMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModContainer {
    public static final DeferredRegister<MenuType<?>> CONTAINER_TYPE = DeferredRegister.create(BuiltInRegistries.MENU, GunMod.MOD_ID);

    public static final net.neoforged.neoforge.registries.DeferredHolder<MenuType<?>, MenuType<GunSmithTableMenu>> GUN_SMITH_TABLE_MENU = CONTAINER_TYPE.register("gun_smith_table_menu", () -> GunSmithTableMenu.TYPE);
    public static final net.neoforged.neoforge.registries.DeferredHolder<MenuType<?>, MenuType<AmmoStorageMenu>> AMMO_STORAGE_MENU = CONTAINER_TYPE.register("ammo_storage_menu",
            () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create((windowId, inv, data) -> new AmmoStorageMenu(windowId, inv)));
}
