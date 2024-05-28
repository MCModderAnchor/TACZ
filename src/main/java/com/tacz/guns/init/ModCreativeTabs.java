package com.tacz.guns.init;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.item.GunTabType;
import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import com.tacz.guns.api.item.builder.AttachmentItemBuilder;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.item.AmmoBoxItem;
import com.tacz.guns.item.AmmoItem;
import com.tacz.guns.item.AttachmentItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

@SuppressWarnings("all")
public class ModCreativeTabs {
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
            .icon(() -> AmmoItemBuilder.create().setId(DefaultAssets.DEFAULT_AMMO_ID).build())
            .displayItems((parameters, output) -> output.acceptAll(AmmoItem.fillItemCategory())).build());

    public static RegistryObject<CreativeModeTab> ATTACHMENT_TAB = TABS.register("attachment", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.tab.tacz.attachment")).icon(() -> AttachmentItemBuilder.create().setId(DefaultAssets.DEFAULT_ATTACHMENT_ID).build())
            .displayItems((parameters, output) -> output.acceptAll(AttachmentItem.fillItemCategory())).build());

    public static RegistryObject<CreativeModeTab> GUN_PISTOL_TAB = TABS.register("pistol", () -> CreativeModeTab.builder()
            .title(Component.translatable("tacz.type.pistol.name"))
            .icon(() -> GunItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "glock_17")).build())
            .displayItems((parameters, output) -> output.acceptAll(AbstractGunItem.fillItemCategory(GunTabType.PISTOL))).build());

    public static RegistryObject<CreativeModeTab> GUN_SNIPER_TAB = TABS.register("sniper", () -> CreativeModeTab.builder()
            .title(Component.translatable("tacz.type.sniper.name"))
            .icon(() -> GunItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "ai_awp")).build())
            .displayItems((parameters, output) -> output.acceptAll(AbstractGunItem.fillItemCategory(GunTabType.SNIPER))).build());

    public static RegistryObject<CreativeModeTab> GUN_RIFLE_TAB = TABS.register("rifle", () -> CreativeModeTab.builder()
            .title(Component.translatable("tacz.type.rifle.name"))
            .icon(() -> GunItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "ak47")).build())
            .displayItems((parameters, output) -> output.acceptAll(AbstractGunItem.fillItemCategory(GunTabType.RIFLE))).build());

    public static RegistryObject<CreativeModeTab> GUN_SHOTGUN_TAB = TABS.register("shotgun", () -> CreativeModeTab.builder()
            .title(Component.translatable("tacz.type.shotgun.name"))
            .icon(() -> GunItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "db_short")).build())
            .displayItems((parameters, output) -> output.acceptAll(AbstractGunItem.fillItemCategory(GunTabType.SHOTGUN))).build());

    public static RegistryObject<CreativeModeTab> GUN_SMG_TAB = TABS.register("smg", () -> CreativeModeTab.builder()
            .title(Component.translatable("tacz.type.smg.name"))
            .icon(() -> GunItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "hk_mp5a5")).build())
            .displayItems((parameters, output) -> output.acceptAll(AbstractGunItem.fillItemCategory(GunTabType.SMG))).build());

    public static RegistryObject<CreativeModeTab> GUN_RPG_TAB = TABS.register("rpg", () -> CreativeModeTab.builder()
            .title(Component.translatable("tacz.type.rpg.name"))
            .icon(() -> GunItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "rpg7")).build())
            .displayItems((parameters, output) -> output.acceptAll(AbstractGunItem.fillItemCategory(GunTabType.RPG))).build());

    public static RegistryObject<CreativeModeTab> GUN_MG_TAB = TABS.register("mg", () -> CreativeModeTab.builder()
            .title(Component.translatable("tacz.type.mg.name"))
            .icon(() -> GunItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "m249")).build())
            .displayItems((parameters, output) -> output.acceptAll(AbstractGunItem.fillItemCategory(GunTabType.MG))).build());

    public static RegistryObject<CreativeModeTab> GUN_OTHER_TAB = TABS.register("unknown", () -> CreativeModeTab.builder()
            .title(Component.translatable("tacz.type.unknown.name"))
            .icon(() -> ItemStack.EMPTY).displayItems((parameters, output) -> output.acceptAll(AbstractGunItem.fillItemCategory(GunTabType.UNKNOWN))).build());
}
