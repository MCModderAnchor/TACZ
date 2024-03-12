package com.tac.guns.init;

import com.tac.guns.GunMod;
import com.tac.guns.inventory.GunSmithTableMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModContainer {
    public static final DeferredRegister<MenuType<?>> CONTAINER_TYPE = DeferredRegister.create(ForgeRegistries.CONTAINERS, GunMod.MOD_ID);

    public static final RegistryObject<MenuType<GunSmithTableMenu>> GUN_SMITH_TABLE_MENU = CONTAINER_TYPE.register("gun_smith_table_menu", () -> GunSmithTableMenu.TYPE);
}
