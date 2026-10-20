package com.tacz.guns.api.item.builder;

import com.google.common.collect.Maps;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.api.item.gun.GunItemManager;
import net.minecraft.core.HolderLookup;
import com.tacz.guns.init.ModItems;
import com.tacz.guns.util.VirtualOemAttachment;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.EnumMap;

public final class GunItemBuilder {
    private int count = 1;
    private int ammoCount = 0;
    private boolean heatData = false;
    private ResourceLocation gunId;
    private FireMode fireMode = FireMode.UNKNOWN;
    private boolean bulletInBarrel = false;
    private EnumMap<AttachmentType, ResourceLocation> attachments = Maps.newEnumMap(AttachmentType.class);
    private EnumMap<AttachmentType, ResourceLocation> virtualAttachments = Maps.newEnumMap(AttachmentType.class);

    private GunItemBuilder() {
    }

    public static GunItemBuilder create() {
        return new GunItemBuilder();
    }

    public GunItemBuilder setCount(int count) {
        this.count = Math.max(count, 1);
        return this;
    }

    public GunItemBuilder setAmmoCount(int count) {
        this.ammoCount = Math.max(count, 0);
        return this;
    }

    public GunItemBuilder setId(ResourceLocation id) {
        this.gunId = id;
        return this;
    }

    public GunItemBuilder setFireMode(FireMode fireMode) {
        this.fireMode = fireMode;
        return this;
    }

    public GunItemBuilder setAmmoInBarrel(boolean ammoInBarrel) {
        this.bulletInBarrel = ammoInBarrel;
        return this;
    }

    public GunItemBuilder putAttachment(AttachmentType type, ResourceLocation attachmentId) {
        this.attachments.put(type, attachmentId);
        return this;
    }

    public GunItemBuilder putAllAttachment(EnumMap<AttachmentType, ResourceLocation> attachments) {
        this.attachments = attachments;
        return this;
    }

    public GunItemBuilder putAllVirtualAttachment(EnumMap<AttachmentType, ResourceLocation> attachments) {
        this.virtualAttachments = attachments;
        return this;
    }

    public GunItemBuilder setHeatData(boolean heatData) {
        this.heatData = heatData;
        return this;
    }

    /**
     * 强行以默认的枪支Item构建一个物品，不进行index检查<br/>
     * 可能会返回功能不完整的物品
     */
    public ItemStack forceBuild(HolderLookup.Provider provider) {
        if (!this.attachments.isEmpty() && provider == null) throw new IllegalArgumentException("'provider' must not be null if attachments are present");
        ItemStack gun = new ItemStack(ModItems.MODERN_KINETIC_GUN.get(), this.count);
        if (gun.getItem() instanceof IGun iGun) {
            iGun.setGunId(gun, this.gunId);
            iGun.setFireMode(gun, this.fireMode);
            iGun.setCurrentAmmoCount(gun, this.ammoCount);
            iGun.setBulletInBarrel(gun, this.bulletInBarrel);
            if(heatData) iGun.setHeatAmount(gun, 0f);
            installAttachments(provider, gun, iGun);
        }
        return gun;
    }

    public ItemStack build() { return build(null); }

    public ItemStack build(HolderLookup.Provider provider) {
        if (!this.attachments.isEmpty() && provider == null) throw new IllegalArgumentException("'provider' must not be null if attachments are present");
        String itemType = TimelessAPI.getCommonGunIndex(gunId).map(index -> index.getPojo().getItemType()).orElse(null);
        if (itemType == null) {
            return ItemStack.EMPTY;
        }

        DeferredItem<? extends AbstractGunItem> gunItemRegistryObject = GunItemManager.getGunItemRegistryObject(itemType);
        if (gunItemRegistryObject == null) {
            return ItemStack.EMPTY;
        }

        ItemStack gun = new ItemStack(gunItemRegistryObject.get(), this.count);
        if (gun.getItem() instanceof IGun iGun) {
            iGun.setGunId(gun, this.gunId);
            iGun.setFireMode(gun, this.fireMode);
            iGun.setCurrentAmmoCount(gun, this.ammoCount);
            iGun.setBulletInBarrel(gun, this.bulletInBarrel);
            installAttachments(provider, gun, iGun);
        }
        return gun;
    }

    private void installAttachments(HolderLookup.Provider provider, ItemStack gun, IGun iGun) {
        this.virtualAttachments.forEach((type, id) -> {
            if (this.attachments.containsKey(type)) {
                return;
            }
            ItemStack attachmentStack = AttachmentItemBuilder.create().setId(id).build();
            VirtualOemAttachment.mark(attachmentStack);
            iGun.installAttachment(provider == null ? net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(net.minecraft.core.registries.BuiltInRegistries.REGISTRY) : provider, gun, attachmentStack);
        });
        this.attachments.forEach((type, id) -> {
            ItemStack attachmentStack = AttachmentItemBuilder.create().setId(id).build();
            iGun.installAttachment(provider == null ? net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(net.minecraft.core.registries.BuiltInRegistries.REGISTRY) : provider, gun, attachmentStack);
        });
    }
}
