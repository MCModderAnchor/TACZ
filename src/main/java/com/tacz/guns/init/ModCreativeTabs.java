package com.tacz.guns.init;

import com.google.common.collect.Maps;
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
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("all")
public class ModCreativeTabs {
    private static Map<ResourceLocation, CreativeModeTab> TABS = Maps.newHashMap();

    public static void initCreativeTabs() {
        addCreativeTabs("other", new TranslatableComponent("itemGroup.tab.tacz.other"),
                () -> ModItems.GUN_SMITH_TABLE.get().getDefaultInstance(),
                output -> {
                    output.add(ModItems.GUN_SMITH_TABLE.get().getDefaultInstance());
                    output.add(ModItems.TARGET.get().getDefaultInstance());
                    output.add(ModItems.STATUE.get().getDefaultInstance());
                    output.add(ModItems.TARGET_MINECART.get().getDefaultInstance());
                    AmmoBoxItem.fillItemCategory(output);
                });

        addCreativeTabs("ammo", new TranslatableComponent("itemGroup.tab.tacz.ammo"),
                () -> AmmoItemBuilder.create().setId(DefaultAssets.DEFAULT_AMMO_ID).build(),
                output -> output.addAll(AmmoItem.fillItemCategory()));

        addCreativeTabs("scope", new TranslatableComponent("tacz.type.scope.name"),
                () -> AttachmentItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "sight_sro_dot")).build(),
                output -> output.addAll(AttachmentItem.fillItemCategory(AttachmentType.SCOPE)));

        addCreativeTabs("muzzle", new TranslatableComponent("tacz.type.muzzle.name"),
                () -> AttachmentItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "muzzle_compensator_trident")).build(),
                output -> output.addAll(AttachmentItem.fillItemCategory(AttachmentType.MUZZLE)));

        addCreativeTabs("stock", new TranslatableComponent("tacz.type.stock.name"),
                () -> AttachmentItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "stock_militech_b5")).build(),
                output -> output.addAll(AttachmentItem.fillItemCategory(AttachmentType.STOCK)));

        addCreativeTabs("grip", new TranslatableComponent("tacz.type.grip.name"),
                () -> AttachmentItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "grip_magpul_afg_2")).build(),
                output -> output.addAll(AttachmentItem.fillItemCategory(AttachmentType.GRIP)));

        addCreativeTabs("extended_mag", new TranslatableComponent("tacz.type.extended_mag.name"),
                () -> AttachmentItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "extended_mag_3")).build(),
                output -> output.addAll(AttachmentItem.fillItemCategory(AttachmentType.EXTENDED_MAG)));

        addCreativeTabs("pistol", new TranslatableComponent("tacz.type.pistol.name"),
                () -> GunItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "glock_17")).build(),
                output -> output.addAll(AbstractGunItem.fillItemCategory(GunTabType.PISTOL)));

        addCreativeTabs("sniper", new TranslatableComponent("tacz.type.sniper.name"),
                () -> GunItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "ai_awp")).build(),
                output -> output.addAll(AbstractGunItem.fillItemCategory(GunTabType.SNIPER)));

        addCreativeTabs("rifle", new TranslatableComponent("tacz.type.rifle.name"),
                () -> GunItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "ak47")).build(),
                output -> output.addAll(AbstractGunItem.fillItemCategory(GunTabType.RIFLE)));

        addCreativeTabs("shotgun", new TranslatableComponent("tacz.type.shotgun.name"),
                () -> GunItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "db_short")).build(),
                output -> output.addAll(AbstractGunItem.fillItemCategory(GunTabType.SHOTGUN)));

        addCreativeTabs("smg", new TranslatableComponent("tacz.type.smg.name"),
                () -> GunItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "hk_mp5a5")).build(),
                output -> output.addAll(AbstractGunItem.fillItemCategory(GunTabType.SMG)));

        addCreativeTabs("rpg", new TranslatableComponent("tacz.type.rpg.name"),
                () -> GunItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "rpg7")).build(),
                output -> output.addAll(AbstractGunItem.fillItemCategory(GunTabType.RPG)));

        addCreativeTabs("mg", new TranslatableComponent("tacz.type.mg.name"),
                () -> GunItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "m249")).build(),
                output -> output.addAll(AbstractGunItem.fillItemCategory(GunTabType.MG)));
    }

    private static void addCreativeTabs(String label, Component displayName, Supplier<ItemStack> iconStack, Consumer<NonNullList<ItemStack>> tabConsumer) {
        GunModTab tab = new GunModTab(label, displayName, iconStack, tabConsumer);
        TABS.put(new ResourceLocation(GunMod.MOD_ID, label), tab);
    }

    public static CreativeModeTab getModTabs(ResourceLocation id) {
        return TABS.get(id);
    }

    private static class GunModTab extends CreativeModeTab {
        private Component displayName;
        private Supplier<ItemStack> iconStack;
        private Consumer<NonNullList<ItemStack>> tabConsumer;

        public GunModTab(String label, Component displayName, Supplier<ItemStack> iconStack, Consumer<NonNullList<ItemStack>> tabConsumer) {
            super(label);
            this.displayName = displayName;
            this.iconStack = iconStack;
            this.tabConsumer = tabConsumer;
        }

        @Override
        public @Nonnull ItemStack makeIcon() {
            return iconStack.get();
        }

        @Override
        public Component getDisplayName() {
            return displayName;
        }

        @Override
        public void fillItemList(NonNullList<ItemStack> items) {
            tabConsumer.accept(items);
        }
    }
}
