package com.tac.guns.init;

import com.tac.guns.GunMod;
import com.tac.guns.item.AmmoItem;
import com.tac.guns.item.GunItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, GunMod.MOD_ID);
    public static CreativeModeTab GUN_TAB = new CreativeModeTab("tab.tac.gun") {
        @Override
        public ItemStack makeIcon() {
            return Items.ACACIA_BOAT.getDefaultInstance();
        }
    };
    public static CreativeModeTab AMMO_TAB = new CreativeModeTab("tab.tac.gun") {
        @Override
        public ItemStack makeIcon() {
            return Items.ACACIA_DOOR.getDefaultInstance();
        }
    };

    public static RegistryObject<Item> GUN = ITEMS.register("gun", GunItem::new);
    public static RegistryObject<Item> AMMO = ITEMS.register("ammo", AmmoItem::new);
}