package com.tacz.guns.init;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.item.gun.GunItemManager;
import com.tacz.guns.item.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, GunMod.MOD_ID);

    public static RegistryObject<ModernKineticGunItem> MODERN_KINETIC_GUN = ITEMS.register("modern_kinetic_gun", ModernKineticGunItem::new);
    public static RegistryObject<Item> AMMO = ITEMS.register("ammo", AmmoItem::new);
    public static RegistryObject<Item> ATTACHMENT = ITEMS.register("attachment", AttachmentItem::new);

    public static RegistryObject<GunSmithTableItem> GUN_SMITH_TABLE = ITEMS.register("gun_smith_table", () -> new DefaultTableItem(ModBlocks.GUN_SMITH_TABLE.get()));
    public static RegistryObject<GunSmithTableItem> WORKBENCH_111 = ITEMS.register("workbench_a", () -> new GunSmithTableItem(ModBlocks.WORKBENCH_111.get()));
    public static RegistryObject<GunSmithTableItem> WORKBENCH_211 = ITEMS.register("workbench_b", () -> new GunSmithTableItem(ModBlocks.WORKBENCH_211.get()));
    public static RegistryObject<GunSmithTableItem> WORKBENCH_121 = ITEMS.register("workbench_c", () -> new GunSmithTableItem(ModBlocks.WORKBENCH_121.get()));

    public static RegistryObject<Item> TARGET = ITEMS.register("target", () -> new BlockItem(ModBlocks.TARGET.get(), new Item.Properties()));
    public static RegistryObject<Item> STATUE = ITEMS.register("statue", () -> new BlockItem(ModBlocks.STATUE.get(), new Item.Properties()));
    public static RegistryObject<Item> AMMO_BOX = ITEMS.register("ammo_box", AmmoBoxItem::new);
    public static RegistryObject<Item> TARGET_MINECART = ITEMS.register("target_minecart", TargetMinecartItem::new);

    @SubscribeEvent
    public static void onItemRegister(RegistryEvent.Register<Item> event) {
        GunItemManager.registerGunItem(ModernKineticGunItem.TYPE_NAME, MODERN_KINETIC_GUN);
        ModCreativeTabs.initCreativeTabs();
    }
}