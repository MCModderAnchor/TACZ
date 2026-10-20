package com.tacz.guns.api.item.nbt;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.builder.AttachmentItemBuilder;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.index.ClientAttachmentIndex;
import com.tacz.guns.client.resource.index.ClientAttachmentVariantIndex;
import com.tacz.guns.resource.index.CommonGunIndex;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

@SuppressWarnings("deprecation")
public interface GunItemDataAccessor extends IGun {
    String GUN_ID_TAG = "GunId";
    String GUN_FIRE_MODE_TAG = "GunFireMode";
    String GUN_HAS_BULLET_IN_BARREL = "HasBulletInBarrel";
    String GUN_CURRENT_AMMO_COUNT_TAG = "GunCurrentAmmoCount";
    String GUN_ATTACHMENT_BASE = "Attachment";
    String GUN_EXP_TAG = "GunLevelExp";
    String GUN_DUMMY_AMMO = "DummyAmmo";
    String GUN_MAX_DUMMY_AMMO = "MaxDummyAmmo";
    String GUN_ATTACHMENT_LOCK = "AttachmentLock";
    String GUN_DISPLAY_ID_TAG = "GunDisplayId";
    String LASER_COLOR_TAG = "LaserColor";
    String GUN_OVERHEAT_TAG = "HeatAmount";
    String GUN_OVERHEAT_LOCK_TAG = "OverHeated";

    @Override
    default boolean useDummyAmmo(ItemStack gun) {
        CompoundTag nbt = gun.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).getUnsafe();
        return nbt.contains(GUN_DUMMY_AMMO, Tag.TAG_INT);
    }

    @Override
    default int getDummyAmmoAmount(ItemStack gun) {
        CompoundTag nbt = gun.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).getUnsafe();
        return Math.max(0, nbt.getInt(GUN_DUMMY_AMMO));
    }

    @Override
    default void setDummyAmmoAmount(ItemStack gun, int amount) {
        gun.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, data -> data.update(tag -> {
            tag.putInt(GUN_DUMMY_AMMO, Math.max(amount, 0));
        }));
    }

    @Override
    default void addDummyAmmoAmount(ItemStack gun, int amount) {
        if (!useDummyAmmo(gun)) {
            return;
        }
        int maxDummyAmmo = Integer.MAX_VALUE;
        if (hasMaxDummyAmmo(gun)) {
            maxDummyAmmo = getMaxDummyAmmoAmount(gun);
        }
        long updatedAmount = Math.min((long) getDummyAmmoAmount(gun) + amount, maxDummyAmmo);
        gun.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, data -> data.update(tag ->
                tag.putInt(GUN_DUMMY_AMMO, (int) Math.max(updatedAmount, 0))));
    }

    @Override
    default boolean hasMaxDummyAmmo(ItemStack gun) {
        CompoundTag nbt = gun.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).getUnsafe();
        return nbt.contains(GUN_MAX_DUMMY_AMMO, Tag.TAG_INT);
    }

    @Override
    default int getMaxDummyAmmoAmount(ItemStack gun) {
        CompoundTag nbt = gun.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).getUnsafe();
        return Math.max(0, nbt.getInt(GUN_MAX_DUMMY_AMMO));
    }

    @Override
    default void setMaxDummyAmmoAmount(ItemStack gun, int amount) {
        gun.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, data -> data.update(tag -> {
            tag.putInt(GUN_MAX_DUMMY_AMMO, Math.max(amount, 0));
        }));
    }

    @Override
    default boolean hasAttachmentLock(ItemStack gun) {
        CompoundTag nbt = gun.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).getUnsafe();
        if (nbt.contains(GUN_ATTACHMENT_LOCK, Tag.TAG_BYTE)) {
            return nbt.getBoolean(GUN_ATTACHMENT_LOCK);
        }
        return false;
    }

    @Override
    default void setAttachmentLock(ItemStack gun, boolean lock) {
        gun.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, data -> data.update(tag -> {
            tag.putBoolean(GUN_ATTACHMENT_LOCK, lock);
        }));
    }

    @Override
    @Nonnull
    default ResourceLocation getGunId(ItemStack gun) {
        CompoundTag nbt = gun.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).getUnsafe();
        if (nbt.contains(GUN_ID_TAG, Tag.TAG_STRING)) {
            ResourceLocation gunId = ResourceLocation.tryParse(nbt.getString(GUN_ID_TAG));
            return Objects.requireNonNullElse(gunId, DefaultAssets.EMPTY_GUN_ID);
        }
        return DefaultAssets.EMPTY_GUN_ID;
    }

    @Override
    default void setGunId(ItemStack gun, @Nullable ResourceLocation gunId) {
        gun.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, data -> data.update(tag -> {
            if (gunId != null) {
                tag.putString(GUN_ID_TAG, gunId.toString());
            }
        }));
    }

    @Override
    @NotNull
    default ResourceLocation getGunDisplayId(ItemStack gun) {
        CompoundTag nbt = gun.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).getUnsafe();
        if (nbt.contains(GUN_DISPLAY_ID_TAG, Tag.TAG_STRING)) {
            ResourceLocation gunDisplayId = ResourceLocation.tryParse(nbt.getString(GUN_DISPLAY_ID_TAG));
            return Objects.requireNonNullElse(gunDisplayId, DefaultAssets.DEFAULT_GUN_DISPLAY_ID);
        }
        return DefaultAssets.DEFAULT_GUN_DISPLAY_ID;
    }

    @Override
    default void setGunDisplayId(ItemStack gun, ResourceLocation displayId) {
        gun.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, data -> data.update(tag -> {
            if (displayId != null) {
                tag.putString(GUN_DISPLAY_ID_TAG, displayId.toString());
            }
        }));
    }

    @Override
    default int getLevel(ItemStack gun) {
        CompoundTag nbt = gun.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).getUnsafe();
        if (nbt.contains(GUN_EXP_TAG, Tag.TAG_INT)) {
            return getLevel(nbt.getInt(GUN_EXP_TAG));
        }
        return 0;
    }

    @Override
    default int getExp(ItemStack gun) {
        CompoundTag nbt = gun.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).getUnsafe();
        if (nbt.contains(GUN_EXP_TAG, Tag.TAG_INT)) {
            return nbt.getInt(GUN_EXP_TAG);
        }
        return 0;
    }

    @Override
    default int getExpToNextLevel(ItemStack gun) {
        int exp = getExp(gun);
        int level = getLevel(exp);
        if (level >= getMaxLevel()) {
            return 0;
        }
        int nextLevelExp = getExp(level + 1);
        return nextLevelExp - exp;
    }

    @Override
    default int getExpCurrentLevel(ItemStack gun) {
        int exp = getExp(gun);
        int level = getLevel(exp);
        if (level <= 0) {
            return exp;
        } else {
            return exp - getExp(level - 1);
        }
    }

    @Override
    default FireMode getFireMode(ItemStack gun) {
        CompoundTag nbt = gun.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).getUnsafe();
        if (nbt.contains(GUN_FIRE_MODE_TAG, Tag.TAG_STRING)) {
            return FireMode.fromName(nbt.getString(GUN_FIRE_MODE_TAG));
        }
        return FireMode.UNKNOWN;
    }

    @Override
    default void setFireMode(ItemStack gun, @Nullable FireMode fireMode) {
        gun.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, data -> data.update(tag -> {
            if (fireMode != null) {
                tag.putString(GUN_FIRE_MODE_TAG, fireMode.name());
                return;
            }
            tag.putString(GUN_FIRE_MODE_TAG, FireMode.UNKNOWN.name());
        }));
    }

    @Override
    default int getCurrentAmmoCount(ItemStack gun) {
        CompoundTag nbt = gun.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).getUnsafe();
        if (nbt.contains(GUN_CURRENT_AMMO_COUNT_TAG, Tag.TAG_INT)) {
            return nbt.getInt(GUN_CURRENT_AMMO_COUNT_TAG);
        }
        return 0;
    }

    @Override
    default void setCurrentAmmoCount(ItemStack gun, int ammoCount) {
        gun.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, data -> data.update(tag -> {
            tag.putInt(GUN_CURRENT_AMMO_COUNT_TAG, Math.max(ammoCount, 0));
        }));
    }

    @Override
    default void reduceCurrentAmmoCount(ItemStack gun) {
        // 只在不使用背包直读的情况下减少 AmmoCount
        if (!useInventoryAmmo(gun)) {
            setCurrentAmmoCount(gun, getCurrentAmmoCount(gun) - 1);
        }
    }

    @Override
    @Nullable
    default CompoundTag getAttachmentTag(ItemStack gun, AttachmentType type) {
        if (!allowAttachmentType(gun, type)) {
            return null;
        }
        CompoundTag nbt = gun.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).getUnsafe();
        String key = GUN_ATTACHMENT_BASE + type.name();
        if (!nbt.contains(key, Tag.TAG_COMPOUND)) return null;
        CompoundTag stack = nbt.getCompound(key);
        if (!stack.contains("components", Tag.TAG_COMPOUND)) return null;
        CompoundTag components = stack.getCompound("components");
        if (!components.contains(DataComponents.CUSTOM_DATA.toString())) return null;
        return components.getCompound(DataComponents.CUSTOM_DATA.toString()).copy();
    }

    @Override
    default void setAttachmentTag(ItemStack gun, AttachmentType type, CompoundTag attachmentTag) {
        if (!allowAttachmentType(gun, type)) {
            return;
        }
        gun.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, data -> data.update(tag -> {
            String key = GUN_ATTACHMENT_BASE + type.name();
            if (!tag.contains(key, Tag.TAG_COMPOUND)) return;
            CompoundTag stack = tag.getCompound(key);
            if (!stack.contains("components", Tag.TAG_COMPOUND)) return;
            CompoundTag components = stack.getCompound("components");
            if (!components.contains(DataComponents.CUSTOM_DATA.toString())) return;
            components.put(DataComponents.CUSTOM_DATA.toString(), attachmentTag);
        }));
    }

    @Override
    @NotNull
    default ItemStack getBuiltinAttachment(ItemStack gun, AttachmentType type) {
        IGun iGun = IGun.getIGunOrNull(gun);
        if (iGun == null) {
            return ItemStack.EMPTY;
        }
        CommonGunIndex index = TimelessAPI.getCommonGunIndex(iGun.getGunId(gun)).orElse(null);
        if (index != null){
            var builtin = index.getGunData().getBuiltInAttachments();
            if (builtin.containsKey(type)) {
                ResourceLocation attachmentId = builtin.get(type).getDisplayAttachment();
                if (attachmentId != null) {
                    return AttachmentItemBuilder.create().setId(attachmentId).build();
                }
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    @Nonnull
    default ItemStack getAttachment(HolderLookup.Provider provider, ItemStack gun, AttachmentType type) {
        if (!allowAttachmentType(gun, type)) {
            return ItemStack.EMPTY;
        }
        CompoundTag nbt = gun.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).getUnsafe();
        String key = GUN_ATTACHMENT_BASE + type.name();
        if (nbt.contains(key, Tag.TAG_COMPOUND)) {
            return ItemStack.CODEC.parse(provider.createSerializationContext(NbtOps.INSTANCE), nbt.getCompound(key)).result().orElse(ItemStack.EMPTY);
        }
        return ItemStack.EMPTY;
    }

    @Override
    @NotNull
    default ResourceLocation getBuiltInAttachmentId(ItemStack gun, AttachmentType type) {
        IGun iGun = IGun.getIGunOrNull(gun);
        if (iGun == null) {
            return DefaultAssets.EMPTY_ATTACHMENT_ID;
        }
        CommonGunIndex index = TimelessAPI.getCommonGunIndex(iGun.getGunId(gun)).orElse(null);
        if (index != null){
            var builtin = index.getGunData().getBuiltInAttachments();
            if (builtin.containsKey(type)) {
                ResourceLocation attachmentId = builtin.get(type).getDisplayAttachment();
                if (attachmentId != null) {
                    return attachmentId;
                }
            }
        }
        return DefaultAssets.EMPTY_ATTACHMENT_ID;
    }

    @Override
    @Nonnull
    default ResourceLocation getAttachmentId(ItemStack gun, AttachmentType type) {
        CompoundTag attachmentTag = this.getAttachmentTag(gun, type);
        if (attachmentTag != null) {
            return AttachmentItemDataAccessor.getAttachmentIdFromTag(attachmentTag);
        }
        return DefaultAssets.EMPTY_ATTACHMENT_ID;
    }

    @Override
    default void installAttachment(HolderLookup.Provider provider, @Nonnull ItemStack gun, @Nonnull ItemStack attachment) {
        if (!allowAttachment(gun, attachment)) {
            return;
        }
        IAttachment iAttachment = IAttachment.getIAttachmentOrNull(attachment);
        if (iAttachment == null) {
            return;
        }
        gun.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, data -> data.update(tag -> {
            String key = GUN_ATTACHMENT_BASE + iAttachment.getType(attachment).name();
            tag.put(key, attachment.saveOptional(provider));
        }));
    }

    @Override
    default void unloadAttachment(HolderLookup.Provider provider, @Nonnull ItemStack gun, AttachmentType type) {
        if (!allowAttachmentType(gun, type)) {
            return;
        }
        gun.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, data -> data.update(tag -> {
            String key = GUN_ATTACHMENT_BASE + type.name();
            tag.put(key, ItemStack.EMPTY.saveOptional(provider));
        }));
    }

    @Override
    default float getAimingZoom(ItemStack gunItem) {
        float zoom = 1;
        ResourceLocation scopeId = this.getAttachmentId(gunItem, AttachmentType.SCOPE);
        boolean builtin = false;
        if (scopeId.equals(DefaultAssets.EMPTY_ATTACHMENT_ID)) {
            scopeId = getBuiltInAttachmentId(gunItem, AttachmentType.SCOPE);
            builtin = true;
        }
        if (!DefaultAssets.isEmptyAttachmentId(scopeId)) {
            CompoundTag attachmentTag = this.getAttachmentTag(gunItem, AttachmentType.SCOPE);
            int zoomNumber = builtin ? 0 : AttachmentItemDataAccessor.getZoomNumberFromTag(attachmentTag);
            ResourceLocation resolvedScopeId = scopeId;
            ResourceLocation variantId = attachmentTag == null ? null : ResourceLocation.tryParse(attachmentTag.getString("Variant"));
            float[] zooms = java.util.Optional.ofNullable(variantId)
                    .filter(id -> TimelessAPI.getCommonAttachmentVariantIndex(id).map(index -> index.appliesTo(resolvedScopeId)).orElse(false))
                    .flatMap(TimelessAPI::getClientAttachmentVariantIndex)
                    .map(ClientAttachmentVariantIndex::getZoom)
                    .orElseGet(() -> TimelessAPI.getClientAttachmentIndex(resolvedScopeId).map(ClientAttachmentIndex::getZoom).orElse(null));
            if (zooms != null) {
                zoom = zooms[zoomNumber % zooms.length];
            }
        } else {
            zoom = TimelessAPI.getGunDisplay(gunItem).map(GunDisplayInstance::getIronZoom).orElse(1f);
        }
        return zoom;
    }

    @Override
    default boolean hasBulletInBarrel(ItemStack gun) {
        CompoundTag nbt = gun.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).getUnsafe();
        if (nbt.contains(GUN_HAS_BULLET_IN_BARREL, Tag.TAG_BYTE)) {
            return nbt.getBoolean(GUN_HAS_BULLET_IN_BARREL);
        }
        return false;
    }

    @Override
    default void setBulletInBarrel(ItemStack gun, boolean bulletInBarrel) {
        gun.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, data -> data.update(tag -> {
            tag.putBoolean(GUN_HAS_BULLET_IN_BARREL, bulletInBarrel);
        }));
    }

    @Override
    default boolean hasCustomLaserColor(ItemStack gun) {
        CompoundTag nbt = gun.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).getUnsafe();
        return nbt.contains(LASER_COLOR_TAG, Tag.TAG_INT);
    }

    @Override
    default int getLaserColor(ItemStack gun) {
        CompoundTag nbt = gun.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).getUnsafe();
        if (!hasCustomLaserColor(gun)) {
            return 0xFF0000;
        }
        return nbt.getInt(LASER_COLOR_TAG);
    }

    @Override
    default void setLaserColor(ItemStack gun, int color) {
        gun.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, data -> data.update(tag -> {
            tag.putInt(LASER_COLOR_TAG, color);
        }));
    }

    /**
     * Heat Data
     */
    @Override
    default boolean hasHeatData(ItemStack gun) {
        return gun.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).getUnsafe().contains(GUN_OVERHEAT_TAG, Tag.TAG_FLOAT);
    }

    @Override
    default boolean isOverheatLocked(ItemStack gun) {
        return gun.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).getUnsafe().getBoolean(GUN_OVERHEAT_LOCK_TAG);
    }

    @Override
    default void setOverheatLocked(ItemStack gun, boolean locked) {
        gun.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, data -> data.update(tag -> {
            tag.putBoolean(GUN_OVERHEAT_LOCK_TAG, locked);
        }));
    }

    @Override
    default float getHeatAmount(ItemStack gun) {
        if(hasHeatData(gun)) return gun.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).getUnsafe().getFloat(GUN_OVERHEAT_TAG);
        return 0f;
    }

    @Override
    default void setHeatAmount(ItemStack gun, float amount) {
        gun.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, data -> data.update(tag -> {
            tag.putFloat(GUN_OVERHEAT_TAG, amount >= 0 ? amount : 0f);
        }));
    }

    @Override
    default float lerpRPM(ItemStack gun) {
        return TimelessAPI.getCommonGunIndex(getGunId(gun))
                .map(index -> index.getGunData().getHeatData())
                .map(heatData -> {
                    float heatPercentage = (getHeatAmount(gun) / heatData.getHeatMax());
                    return Mth.lerp(heatPercentage, heatData.getMinRpmMod(), heatData.getMaxRpmMod());
                }).orElse(1f);
    }

    @Override
    default float lerpInaccuracy(ItemStack gun) {
        return TimelessAPI.getCommonGunIndex(getGunId(gun))
                .map(index -> index.getGunData().getHeatData())
                .map(heatData -> {
                    float heatPercentage = (getHeatAmount(gun) / heatData.getHeatMax());
                    return Mth.lerp(heatPercentage, heatData.getMinInaccuracy(), heatData.getMaxInaccuracy());
                }).orElse(1f);
    }
}
