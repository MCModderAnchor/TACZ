package com.tacz.guns.init;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import com.tacz.guns.api.item.builder.AttachmentItemBuilder;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.item.AmmoBoxItem;
import com.tacz.guns.item.AmmoItem;
import com.tacz.guns.item.AttachmentItem;
import com.tacz.guns.item.ModernKineticGunItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static ResourceLocation DEFAULT_GUN_ID = new ResourceLocation(GunMod.MOD_ID, "ak47");

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, GunMod.MOD_ID);

    public static RegistryObject<CreativeModeTab> OTHER_TAB = TABS.register("other", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.tab.tacz.other"))
            .icon(() -> ModItems.GUN_SMITH_TABLE.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(ModItems.GUN_SMITH_TABLE.get());
                output.accept(ModItems.TARGET.get());
                output.accept(ModItems.TARGET_MINECART.get());
                AmmoBoxItem.fillItemCategory(output);
            }).build());

    public static RegistryObject<CreativeModeTab> AMMO_TAB = TABS.register("ammo", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.tab.tacz.ammo"))
            .icon(() -> AmmoItemBuilder.create().setId(DefaultAssets.DEFAULT_AMMO_ID)
                    .build()).displayItems((parameters, output) -> {
                output.acceptAll(AmmoItem.fillItemCategory());
            }).build());

    public static RegistryObject<CreativeModeTab> ATTACHMENT_TAB = TABS.register("attachment", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.tab.tacz.attachment")).icon(() -> AttachmentItemBuilder.create()
                    .setId(DefaultAssets.DEFAULT_ATTACHMENT_ID).build())
            .displayItems((parameters, output) -> {
                output.acceptAll(AttachmentItem.fillItemCategory());
            }).build());

    public static RegistryObject<CreativeModeTab> GUN_TAB = TABS.register("gun", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.tab.tacz.attachment")).icon(() -> GunItemBuilder.create()
                    .setId(DEFAULT_GUN_ID).build())
            .displayItems((parameters, output) -> {
                output.acceptAll(ModernKineticGunItem.fillItemCategory());
            }).build());
}
