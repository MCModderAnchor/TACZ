package com.tacz.guns.init;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.item.gun.GunItemManager;
import com.tacz.guns.item.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

import javax.annotation.Nullable;
import java.util.List;

@net.neoforged.fml.common.EventBusSubscriber(bus = net.neoforged.fml.common.EventBusSubscriber.Bus.MOD)
public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(GunMod.MOD_ID);

    public static DeferredItem<ModernKineticGunItem> MODERN_KINETIC_GUN = ITEMS.register("modern_kinetic_gun", ModernKineticGunItem::new);

//    public static RegistryObject<ThrowableItem> M67 = ITEMS.register("m67", ThrowableItem::new);

    public static DeferredItem<Item> AMMO = ITEMS.register("ammo", AmmoItem::new);
    public static DeferredItem<AttachmentItem> ATTACHMENT = ITEMS.register("attachment", AttachmentItem::new);

    public static DeferredItem<GunSmithTableItem> GUN_SMITH_TABLE = ITEMS.register("gun_smith_table", () -> new DefaultTableItem(ModBlocks.GUN_SMITH_TABLE.get()));
    public static DeferredItem<GunSmithTableItem> WORKBENCH_111 = ITEMS.register("workbench_a", () -> new GunSmithTableItem(ModBlocks.WORKBENCH_111.get()));
    public static DeferredItem<GunSmithTableItem> WORKBENCH_211 = ITEMS.register("workbench_b", () -> new GunSmithTableItem(ModBlocks.WORKBENCH_211.get()));
    public static DeferredItem<GunSmithTableItem> WORKBENCH_121 = ITEMS.register("workbench_c", () -> new GunSmithTableItem(ModBlocks.WORKBENCH_121.get()));


    public static net.neoforged.neoforge.registries.DeferredItem<Item> TARGET = ITEMS.register("target", () -> new BlockItem(ModBlocks.TARGET.get(), new Item.Properties()));
    public static net.neoforged.neoforge.registries.DeferredItem<Item> STATUE = ITEMS.register("statue", () -> new BlockItem(ModBlocks.STATUE.get(), new Item.Properties()));
    public static net.neoforged.neoforge.registries.DeferredItem<Item> GUN_PEGBOARD = ITEMS.register("gun_pegboard", () -> new GunPegboardItem(ModBlocks.GUN_PEGBOARD.get(), new Item.Properties()));
    public static net.neoforged.neoforge.registries.DeferredItem<Item> GLOW_GUN_PEGBOARD = ITEMS.register("glow_gun_pegboard", () -> new GunPegboardItem(ModBlocks.GLOW_GUN_PEGBOARD.get(), new Item.Properties()));
    public static net.neoforged.neoforge.registries.DeferredItem<Item> AMMO_STORAGE = ITEMS.register("ammo_storage", () -> new AmmoStorageBlockItem(ModBlocks.AMMO_STORAGE.get()));
    public static net.neoforged.neoforge.registries.DeferredItem<Item> AMMO_STORAGE_GOLD = ITEMS.register("ammo_storage_gold", () -> new AmmoStorageBlockItem(ModBlocks.AMMO_STORAGE_GOLD.get()));
    public static net.neoforged.neoforge.registries.DeferredItem<Item> AMMO_STORAGE_DIAMOND = ITEMS.register("ammo_storage_diamond", () -> new AmmoStorageBlockItem(ModBlocks.AMMO_STORAGE_DIAMOND.get()));
    public static net.neoforged.neoforge.registries.DeferredItem<Item> GUN_DISPLAY_CASE = ITEMS.register("gun_display_case", () -> new BlockItem(ModBlocks.GUN_DISPLAY_CASE.get(), new Item.Properties()) {
        @Override
        public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable("tooltip.tacz.gun_display_case").withStyle(ChatFormatting.GRAY));
        }
    });
    public static net.neoforged.neoforge.registries.DeferredItem<Item> AMMO_BOX = ITEMS.register("ammo_box", AmmoBoxItem::new);
    public static net.neoforged.neoforge.registries.DeferredItem<Item> TARGET_MINECART = ITEMS.register("target_minecart", TargetMinecartItem::new);

    @SubscribeEvent
    public static void onItemRegister(RegisterEvent event) {
        if (event.getRegistryKey().equals(BuiltInRegistries.ITEM.key())) {
            GunItemManager.registerGunItem(ModernKineticGunItem.TYPE_NAME, MODERN_KINETIC_GUN);
        }
    }
}
