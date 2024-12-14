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
import com.tacz.guns.resource.index.CommonGunIndex;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

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

    @Override
    default boolean useDummyAmmo(ItemStack gun) {
        CompoundTag nbt = gun.getOrCreateTag();
        return nbt.contains(GUN_DUMMY_AMMO, Tag.TAG_INT);
    }

    @Override
    default int getDummyAmmoAmount(ItemStack gun) {
        CompoundTag nbt = gun.getOrCreateTag();
        return Math.max(0, nbt.getInt(GUN_DUMMY_AMMO));
    }

    @Override
    default void setDummyAmmoAmount(ItemStack gun, int amount) {
        CompoundTag nbt = gun.getOrCreateTag();
        nbt.putInt(GUN_DUMMY_AMMO, Math.max(amount, 0));
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
        CompoundTag nbt = gun.getOrCreateTag();
        amount = Math.min(getDummyAmmoAmount(gun) + amount, maxDummyAmmo);
        nbt.putInt(GUN_DUMMY_AMMO, Math.max(amount, 0));
    }

    @Override
    default boolean hasMaxDummyAmmo(ItemStack gun) {
        CompoundTag nbt = gun.getOrCreateTag();
        return nbt.contains(GUN_MAX_DUMMY_AMMO, Tag.TAG_INT);
    }

    @Override
    default int getMaxDummyAmmoAmount(ItemStack gun) {
        CompoundTag nbt = gun.getOrCreateTag();
        return Math.max(0, nbt.getInt(GUN_MAX_DUMMY_AMMO));
    }

    @Override
    default void setMaxDummyAmmoAmount(ItemStack gun, int amount) {
        CompoundTag nbt = gun.getOrCreateTag();
        nbt.putInt(GUN_MAX_DUMMY_AMMO, Math.max(amount, 0));
    }

    @Override
    default boolean hasAttachmentLock(ItemStack gun) {
        CompoundTag nbt = gun.getOrCreateTag();
        if (nbt.contains(GUN_ATTACHMENT_LOCK, Tag.TAG_BYTE)) {
            return nbt.getBoolean(GUN_ATTACHMENT_LOCK);
        }
        return false;
    }

    @Override
    default void setAttachmentLock(ItemStack gun, boolean lock) {
        CompoundTag nbt = gun.getOrCreateTag();
        nbt.putBoolean(GUN_ATTACHMENT_LOCK, lock);
    }

    @Override
    @Nonnull
    default ResourceLocation getGunId(ItemStack gun) {
        CompoundTag nbt = gun.getOrCreateTag();
        if (nbt.contains(GUN_ID_TAG, Tag.TAG_STRING)) {
            ResourceLocation gunId = ResourceLocation.tryParse(nbt.getString(GUN_ID_TAG));
            return Objects.requireNonNullElse(gunId, DefaultAssets.EMPTY_GUN_ID);
        }
        return DefaultAssets.EMPTY_GUN_ID;
    }

    @Override
    default void setGunId(ItemStack gun, @Nullable ResourceLocation gunId) {
        CompoundTag nbt = gun.getOrCreateTag();
        if (gunId != null) {
            nbt.putString(GUN_ID_TAG, gunId.toString());
        }
    }

    @Override
    @NotNull
    default ResourceLocation getGunDisplayId(ItemStack gun) {
        CompoundTag nbt = gun.getOrCreateTag();
        if (nbt.contains(GUN_DISPLAY_ID_TAG, Tag.TAG_STRING)) {
            ResourceLocation gunDisplayId = ResourceLocation.tryParse(nbt.getString(GUN_DISPLAY_ID_TAG));
            return Objects.requireNonNullElse(gunDisplayId, DefaultAssets.DEFAULT_GUN_DISPLAY_ID);
        }
        return DefaultAssets.DEFAULT_GUN_DISPLAY_ID;
    }

    @Override
    default void setGunDisplayId(ItemStack gun, ResourceLocation displayId) {
        CompoundTag nbt = gun.getOrCreateTag();
        if (displayId != null) {
            nbt.putString(GUN_DISPLAY_ID_TAG, displayId.toString());
        }
    }

    @Override
    default int getLevel(ItemStack gun) {
        CompoundTag nbt = gun.getOrCreateTag();
        if (nbt.contains(GUN_EXP_TAG, Tag.TAG_INT)) {
            return getLevel(nbt.getInt(GUN_EXP_TAG));
        }
        return 0;
    }

    @Override
    default int getExp(ItemStack gun) {
        CompoundTag nbt = gun.getOrCreateTag();
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
        CompoundTag nbt = gun.getOrCreateTag();
        if (nbt.contains(GUN_FIRE_MODE_TAG, Tag.TAG_STRING)) {
            return FireMode.valueOf(nbt.getString(GUN_FIRE_MODE_TAG));
        }
        return FireMode.UNKNOWN;
    }

    @Override
    default void setFireMode(ItemStack gun, @Nullable FireMode fireMode) {
        CompoundTag nbt = gun.getOrCreateTag();
        if (fireMode != null) {
            nbt.putString(GUN_FIRE_MODE_TAG, fireMode.name());
            return;
        }
        nbt.putString(GUN_FIRE_MODE_TAG, FireMode.UNKNOWN.name());
    }

    @Override
    default int getCurrentAmmoCount(ItemStack gun) {
        CompoundTag nbt = gun.getOrCreateTag();
        if (nbt.contains(GUN_CURRENT_AMMO_COUNT_TAG, Tag.TAG_INT)) {
            return nbt.getInt(GUN_CURRENT_AMMO_COUNT_TAG);
        }
        return 0;
    }

    @Override
    default void setCurrentAmmoCount(ItemStack gun, int ammoCount) {
        CompoundTag nbt = gun.getOrCreateTag();
        nbt.putInt(GUN_CURRENT_AMMO_COUNT_TAG, Math.max(ammoCount, 0));
    }

    @Override
    default void reduceCurrentAmmoCount(ItemStack gun) {
        setCurrentAmmoCount(gun, getCurrentAmmoCount(gun) - 1);
    }

    @Override
    @Nullable
    default CompoundTag getAttachmentTag(ItemStack gun, AttachmentType type) {
        if (!allowAttachmentType(gun, type)) {
            return null;
        }
        CompoundTag nbt = gun.getOrCreateTag();
        String key = GUN_ATTACHMENT_BASE + type.name();
        if (nbt.contains(key, Tag.TAG_COMPOUND)) {
            CompoundTag allItemStackTag = nbt.getCompound(key);
            if (allItemStackTag.contains("tag", Tag.TAG_COMPOUND)) {
                return allItemStackTag.getCompound("tag");
            }
        }
        return null;
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
                return AttachmentItemBuilder.create().setId(builtin.get(type)).build();
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    @Nonnull
    default ItemStack getAttachment(ItemStack gun, AttachmentType type) {
        if (!allowAttachmentType(gun, type)) {
            return ItemStack.EMPTY;
        }
        CompoundTag nbt = gun.getOrCreateTag();
        String key = GUN_ATTACHMENT_BASE + type.name();
        if (nbt.contains(key, Tag.TAG_COMPOUND)) {
            return ItemStack.of(nbt.getCompound(key));
        }
        return ItemStack.EMPTY;
    }

    @Override
    @NotNull
    default  ResourceLocation getBuiltInAttachmentId(ItemStack gun, AttachmentType type) {
        IGun iGun = IGun.getIGunOrNull(gun);
        if (iGun == null) {
            return DefaultAssets.EMPTY_ATTACHMENT_ID;
        }
        CommonGunIndex index = TimelessAPI.getCommonGunIndex(iGun.getGunId(gun)).orElse(null);
        if (index != null){
            var builtin = index.getGunData().getBuiltInAttachments();
            if (builtin.containsKey(type)) {
                return builtin.get(type);
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
    default void installAttachment(@Nonnull ItemStack gun, @Nonnull ItemStack attachment) {
        if (!allowAttachment(gun, attachment)) {
            return;
        }
        IAttachment iAttachment = IAttachment.getIAttachmentOrNull(attachment);
        if (iAttachment == null) {
            return;
        }
        CompoundTag nbt = gun.getOrCreateTag();
        String key = GUN_ATTACHMENT_BASE + iAttachment.getType(attachment).name();
        CompoundTag attachmentTag = new CompoundTag();
        attachment.save(attachmentTag);
        nbt.put(key, attachmentTag);
    }

    @Override
    default void unloadAttachment(@Nonnull ItemStack gun, AttachmentType type) {
        if (!allowAttachmentType(gun, type)) {
            return;
        }
        CompoundTag nbt = gun.getOrCreateTag();
        String key = GUN_ATTACHMENT_BASE + type.name();
        CompoundTag attachmentTag = new CompoundTag();
        ItemStack.EMPTY.save(attachmentTag);
        nbt.put(key, attachmentTag);
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
            float[] zooms = TimelessAPI.getClientAttachmentIndex(scopeId).map(ClientAttachmentIndex::getZoom).orElse(null);
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
        CompoundTag nbt = gun.getOrCreateTag();
        if (nbt.contains(GUN_HAS_BULLET_IN_BARREL, Tag.TAG_BYTE)) {
            return nbt.getBoolean(GUN_HAS_BULLET_IN_BARREL);
        }
        return false;
    }

    @Override
    default void setBulletInBarrel(ItemStack gun, boolean bulletInBarrel) {
        CompoundTag nbt = gun.getOrCreateTag();
        nbt.putBoolean(GUN_HAS_BULLET_IN_BARREL, bulletInBarrel);
    }
}
