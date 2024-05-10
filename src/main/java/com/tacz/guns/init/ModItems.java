package com.tacz.guns.init;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.item.gun.GunItemManager;
import com.tacz.guns.item.*;
import com.tacz.guns.item.builder.AmmoItemBuilder;
import com.tacz.guns.item.builder.AttachmentItemBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nonnull;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, GunMod.MOD_ID);
    public static final ResourceLocation ATTACHMENT_TAB_ICON_ID = new ResourceLocation(GunMod.MOD_ID, "sight_sro_dot");
    public static final ResourceLocation AMMO_TAB_ICON_ID = new ResourceLocation(GunMod.MOD_ID, "762x39");

    public static CreativeModeTab OTHER_TAB = new CreativeModeTab("tab.tacz.other") {
        @Override
        public @Nonnull ItemStack makeIcon() {
            return ModItems.GUN_SMITH_TABLE.get().getDefaultInstance();
        }
    };
    public static CreativeModeTab AMMO_TAB = new CreativeModeTab("tab.tacz.ammo") {
        @Override
        public @Nonnull ItemStack makeIcon() {
            return AmmoItemBuilder.create().setId(AMMO_TAB_ICON_ID).build();
        }
    };
    public static CreativeModeTab ATTACHMENT_TAB = new CreativeModeTab("tab.tacz.attachment") {
        @Override
        public @Nonnull ItemStack makeIcon() {
            return AttachmentItemBuilder.create().setId(ATTACHMENT_TAB_ICON_ID).build();
        }
    };

    public static RegistryObject<ModernKineticGunItem> MODERN_KINETIC_GUN = ITEMS.register("modern_kinetic_gun", ModernKineticGunItem::new);

    public static RegistryObject<Item> AMMO = ITEMS.register("ammo", AmmoItem::new);
    public static RegistryObject<Item> ATTACHMENT = ITEMS.register("attachment", AttachmentItem::new);
    public static RegistryObject<Item> GUN_SMITH_TABLE = ITEMS.register("gun_smith_table", GunSmithTableItem::new);
    public static RegistryObject<Item> TARGET = ITEMS.register("target", () -> new BlockItem(ModBlocks.TARGET.get(), new Item.Properties().tab(OTHER_TAB)));
    public static RegistryObject<Item> AMMO_BOX = ITEMS.register("ammo_box", AmmoBoxItem::new);
    public static RegistryObject<Item> TARGET_MINECART = ITEMS.register("target_minecart", TargetMinecartItem::new);

    @SubscribeEvent
    public static void onItemRegister(RegistryEvent.Register<Item> event) {
        GunItemManager.registerGunItem(ModernKineticGunItem.TYPE_NAME, MODERN_KINETIC_GUN);
    }
}