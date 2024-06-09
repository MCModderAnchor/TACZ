package com.tacz.guns.init;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.item.GunTabType;
import com.tacz.guns.api.item.attachment.AttachmentType;
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
            .title(Component.translatable("itemGroup.tab.tacz.ammo")).withTabsBefore(OTHER_TAB.getId())
            .icon(() -> AmmoItemBuilder.create().setId(DefaultAssets.DEFAULT_AMMO_ID).build())
            .displayItems((parameters, output) -> output.acceptAll(AmmoItem.fillItemCategory())).build());

    public static RegistryObject<CreativeModeTab> ATTACHMENT_SCOPE_TAB = TABS.register("scope", () -> CreativeModeTab.builder()
            .title(Component.translatable("tacz.type.scope.name")).withTabsBefore(AMMO_TAB.getId())
            .icon(() -> AttachmentItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "sight_sro_dot")).build())
            .displayItems((parameters, output) -> output.acceptAll(AttachmentItem.fillItemCategory(AttachmentType.SCOPE))).build());

    public static RegistryObject<CreativeModeTab> ATTACHMENT_MUZZLE_TAB = TABS.register("muzzle", () -> CreativeModeTab.builder()
            .title(Component.translatable("tacz.type.muzzle.name")).withTabsBefore(ATTACHMENT_SCOPE_TAB.getId())
            .icon(() -> AttachmentItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "muzzle_compensator_trident")).build())
            .displayItems((parameters, output) -> output.acceptAll(AttachmentItem.fillItemCategory(AttachmentType.MUZZLE))).build());

    public static RegistryObject<CreativeModeTab> ATTACHMENT_STOCK_TAB = TABS.register("stock", () -> CreativeModeTab.builder()
            .title(Component.translatable("tacz.type.stock.name")).withTabsBefore(ATTACHMENT_MUZZLE_TAB.getId())
            .icon(() -> AttachmentItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "stock_militech_b5")).build())
            .displayItems((parameters, output) -> output.acceptAll(AttachmentItem.fillItemCategory(AttachmentType.STOCK))).build());

    public static RegistryObject<CreativeModeTab> ATTACHMENT_GRIP_TAB = TABS.register("grip", () -> CreativeModeTab.builder()
            .title(Component.translatable("tacz.type.grip.name")).withTabsBefore(ATTACHMENT_STOCK_TAB.getId())
            .icon(() -> AttachmentItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "grip_magpul_afg_2")).build())
            .displayItems((parameters, output) -> output.acceptAll(AttachmentItem.fillItemCategory(AttachmentType.GRIP))).build());

    public static RegistryObject<CreativeModeTab> ATTACHMENT_EXTENDED_MAG_TAB = TABS.register("extended_mag", () -> CreativeModeTab.builder()
            .title(Component.translatable("tacz.type.extended_mag.name")).withTabsBefore(ATTACHMENT_GRIP_TAB.getId())
            .icon(() -> AttachmentItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "extended_mag_3")).build())
            .displayItems((parameters, output) -> output.acceptAll(AttachmentItem.fillItemCategory(AttachmentType.EXTENDED_MAG))).build());

//    public static RegistryObject<CreativeModeTab> ATTACHMENT_LASER_TAB = TABS.register("laser", () -> CreativeModeTab.builder()
//            .title(Component.translatable("tacz.type.laser.name")).withTabsBefore(ATTACHMENT_EXTENDED_MAG_TAB.getId())
//            .icon(() -> AttachmentItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "sight_sro_dot")).build())
//            .displayItems((parameters, output) -> output.acceptAll(AttachmentItem.fillItemCategory(AttachmentType.LASER))).build());

    public static RegistryObject<CreativeModeTab> GUN_PISTOL_TAB = TABS.register("pistol", () -> CreativeModeTab.builder()
            .title(Component.translatable("tacz.type.pistol.name")).withTabsBefore(ATTACHMENT_EXTENDED_MAG_TAB.getId())
            .icon(() -> GunItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "glock_17")).build())
            .displayItems((parameters, output) -> output.acceptAll(AbstractGunItem.fillItemCategory(GunTabType.PISTOL))).build());

    public static RegistryObject<CreativeModeTab> GUN_SNIPER_TAB = TABS.register("sniper", () -> CreativeModeTab.builder()
            .title(Component.translatable("tacz.type.sniper.name")).withTabsBefore(GUN_PISTOL_TAB.getId())
            .icon(() -> GunItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "ai_awp")).build())
            .displayItems((parameters, output) -> output.acceptAll(AbstractGunItem.fillItemCategory(GunTabType.SNIPER))).build());

    public static RegistryObject<CreativeModeTab> GUN_RIFLE_TAB = TABS.register("rifle", () -> CreativeModeTab.builder()
            .title(Component.translatable("tacz.type.rifle.name")).withTabsBefore(GUN_SNIPER_TAB.getId())
            .icon(() -> GunItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "ak47")).build())
            .displayItems((parameters, output) -> output.acceptAll(AbstractGunItem.fillItemCategory(GunTabType.RIFLE))).build());

    public static RegistryObject<CreativeModeTab> GUN_SHOTGUN_TAB = TABS.register("shotgun", () -> CreativeModeTab.builder()
            .title(Component.translatable("tacz.type.shotgun.name")).withTabsBefore(GUN_RIFLE_TAB.getId())
            .icon(() -> GunItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "db_short")).build())
            .displayItems((parameters, output) -> output.acceptAll(AbstractGunItem.fillItemCategory(GunTabType.SHOTGUN))).build());

    public static RegistryObject<CreativeModeTab> GUN_SMG_TAB = TABS.register("smg", () -> CreativeModeTab.builder()
            .title(Component.translatable("tacz.type.smg.name")).withTabsBefore(GUN_SHOTGUN_TAB.getId())
            .icon(() -> GunItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "hk_mp5a5")).build())
            .displayItems((parameters, output) -> output.acceptAll(AbstractGunItem.fillItemCategory(GunTabType.SMG))).build());

    public static RegistryObject<CreativeModeTab> GUN_RPG_TAB = TABS.register("rpg", () -> CreativeModeTab.builder()
            .title(Component.translatable("tacz.type.rpg.name")).withTabsBefore(GUN_SMG_TAB.getId())
            .icon(() -> GunItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "rpg7")).build())
            .displayItems((parameters, output) -> output.acceptAll(AbstractGunItem.fillItemCategory(GunTabType.RPG))).build());

    public static RegistryObject<CreativeModeTab> GUN_MG_TAB = TABS.register("mg", () -> CreativeModeTab.builder()
            .title(Component.translatable("tacz.type.mg.name")).withTabsBefore(GUN_RPG_TAB.getId())
            .icon(() -> GunItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "m249")).build())
            .displayItems((parameters, output) -> output.acceptAll(AbstractGunItem.fillItemCategory(GunTabType.MG))).build());
}
