package com.tac.guns.init;

import com.tac.guns.GunMod;
import com.tac.guns.item.*;
import com.tac.guns.item.builder.AmmoItemBuilder;
import com.tac.guns.item.builder.AttachmentItemBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nonnull;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, GunMod.MOD_ID);
    public static final ResourceLocation ATTACHMENT_TAB_ICON_ID = new ResourceLocation(GunMod.MOD_ID, "sro_dot");
    public static final ResourceLocation AMMO_TAB_ICON_ID = new ResourceLocation(GunMod.MOD_ID, "762x39");
    public static CreativeModeTab OTHER_TAB = new CreativeModeTab("tab.tac.other") {
        @Override
        public @Nonnull ItemStack makeIcon() {
            return ModItems.GUN_SMITH_TABLE.get().getDefaultInstance();
        }
    };
    public static CreativeModeTab AMMO_TAB = new CreativeModeTab("tab.tac.ammo") {
        @Override
        public @Nonnull ItemStack makeIcon() {
            return AmmoItemBuilder.create().setId(AMMO_TAB_ICON_ID).build();
        }
    };
    public static CreativeModeTab ATTACHMENT_TAB = new CreativeModeTab("tab.tac.attachment") {
        @Override
        public @Nonnull ItemStack makeIcon() {
            return AttachmentItemBuilder.create().setId(ATTACHMENT_TAB_ICON_ID).build();
        }
    };

    public static RegistryObject<Item> GUN = ITEMS.register("gun", GunItem::new);
    public static RegistryObject<Item> AMMO = ITEMS.register("ammo", AmmoItem::new);
    public static RegistryObject<Item> ATTACHMENT = ITEMS.register("attachment", AttachmentItem::new);
    public static RegistryObject<Item> GUN_SMITH_TABLE = ITEMS.register("gun_smith_table", GunSmithTableItem::new);
    public static RegistryObject<Item> TARGET = ITEMS.register("target", () -> new BlockItem(ModBlocks.TARGET.get(), new Item.Properties().tab(OTHER_TAB)));
    public static RegistryObject<Item> AMMO_BOX = ITEMS.register("ammo_box", AmmoBoxItem::new);
    public static RegistryObject<Item> TARGET_MINECART = ITEMS.register("target_minecart", TargetMinecartItem::new);
}