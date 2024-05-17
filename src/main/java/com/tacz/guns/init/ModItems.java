package com.tacz.guns.init;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import com.tacz.guns.api.item.builder.AttachmentItemBuilder;
import com.tacz.guns.api.item.gun.GunItemManager;
import com.tacz.guns.item.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, GunMod.MOD_ID);
    public static final ResourceLocation ATTACHMENT_TAB_ICON_ID = new ResourceLocation(GunMod.MOD_ID, "sight_sro_dot");
    public static final ResourceLocation AMMO_TAB_ICON_ID = new ResourceLocation(GunMod.MOD_ID, "762x39");

    public static CreativeModeTab OTHER_TAB = CreativeModeTab.builder().title(
            Component.translatable("itemGroup.tab.tacz.other")
    ).icon(
            () -> ModItems.GUN_SMITH_TABLE.get().getDefaultInstance()
    ).displayItems(
            (parameters, output) -> {
                output.accept(ModItems.GUN_SMITH_TABLE.get());
                output.accept(ModItems.TARGET.get());
                output.acceptAll(ModItems.ATTACHMENT.get().fillItemCategory());
            }
    ).build();

    public static CreativeModeTab AMMO_TAB = CreativeModeTab.builder().title(
            Component.translatable("itemGroup.tab.tacz.ammo")
    ).icon(
            () -> AmmoItemBuilder.create().setId(AMMO_TAB_ICON_ID).build()
    ).displayItems(
            (parameters, output) -> {
                output.accept(ModItems.AMMO.get());
                output.accept(ModItems.AMMO_BOX.get());
            }
    ).build();

    public static CreativeModeTab ATTACHMENT_TAB = CreativeModeTab.builder().title(
            Component.translatable("itemGroup.tab.tacz.attachment")
    ).icon(
            () -> AttachmentItemBuilder.create().setId(ATTACHMENT_TAB_ICON_ID).build()
    ).displayItems(
            (parameters, output) -> {
                output.accept(ModItems.ATTACHMENT.get());
            }
    ).build();

    public static RegistryObject<ModernKineticGunItem> MODERN_KINETIC_GUN = ITEMS.register("modern_kinetic_gun", ModernKineticGunItem::new);

    public static RegistryObject<Item> AMMO = ITEMS.register("ammo", AmmoItem::new);
    public static RegistryObject<AttachmentItem> ATTACHMENT = ITEMS.register("attachment", AttachmentItem::new);
    public static RegistryObject<Item> GUN_SMITH_TABLE = ITEMS.register("gun_smith_table", GunSmithTableItem::new);
    public static RegistryObject<Item> TARGET = ITEMS.register("target", () -> new BlockItem(ModBlocks.TARGET.get(), new Item.Properties()));
    public static RegistryObject<Item> AMMO_BOX = ITEMS.register("ammo_box", AmmoBoxItem::new);
    public static RegistryObject<Item> TARGET_MINECART = ITEMS.register("target_minecart", TargetMinecartItem::new);

    @SubscribeEvent
    public static void onItemRegister(RegisterEvent event) {
        if(event.getRegistryKey().equals(ForgeRegistries.ITEMS.getRegistryKey())){
            GunItemManager.registerGunItem(ModernKineticGunItem.TYPE_NAME, MODERN_KINETIC_GUN);
        }
    }
}