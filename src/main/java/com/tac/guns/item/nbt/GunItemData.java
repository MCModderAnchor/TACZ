package com.tac.guns.item.nbt;

import com.tac.guns.GunMod;
import com.tac.guns.api.gun.FireMode;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public class GunItemData {
    public static final ResourceLocation DEFAULT = new ResourceLocation(GunMod.MOD_ID, "ak47");
    public static final ResourceLocation DEFAULT_DISPLAY = new ResourceLocation(GunMod.MOD_ID, "ak47_display");
    public static final ResourceLocation DEFAULT_DATA = new ResourceLocation(GunMod.MOD_ID, "ak47_data");

    public static final String GUN_ID_TAG = "GunId";
    public static final String GUN_FIRE_MODE_TAG = "GunFireMode";
    public static final String GUN_MAX_AMMO_COUNT_TAG = "GunMaxAmmoCount";
    public static final String GUN_CURRENT_AMMO_COUNT_TAG = "GunCurrentAmmoCount";

    private @Nullable ResourceLocation gunId = null;
    private FireMode fireMode = FireMode.UNKNOWN;
    private int maxAmmoCount = 1;
    private int currentAmmoCount = 1;

    public static void serialization(@Nonnull CompoundTag nbt, @Nonnull GunItemData data) {
        if (data.gunId != null) {
            nbt.putString(GUN_ID_TAG, data.gunId.toString());
        }
        if (data.fireMode != null) {
            nbt.putString(GUN_FIRE_MODE_TAG, data.fireMode.name());
        }
        nbt.putInt(GUN_MAX_AMMO_COUNT_TAG, data.maxAmmoCount);
        nbt.putInt(GUN_CURRENT_AMMO_COUNT_TAG, data.currentAmmoCount);
    }

    public static @Nonnull GunItemData deserialization(@Nonnull CompoundTag nbt) {
        GunItemData data = new GunItemData();
        if (nbt.contains(GUN_ID_TAG, Tag.TAG_STRING)) {
            data.gunId = ResourceLocation.tryParse(nbt.getString(GUN_ID_TAG));
        }
        if (nbt.contains(GUN_FIRE_MODE_TAG, Tag.TAG_STRING)) {
            data.fireMode = FireMode.valueOf(nbt.getString(GUN_FIRE_MODE_TAG));
        }
        if (nbt.contains(GUN_MAX_AMMO_COUNT_TAG, Tag.TAG_INT)) {
            data.maxAmmoCount = nbt.getInt(GUN_MAX_AMMO_COUNT_TAG);
        }
        if (nbt.contains(GUN_CURRENT_AMMO_COUNT_TAG, Tag.TAG_INT)) {
            data.currentAmmoCount = nbt.getInt(GUN_CURRENT_AMMO_COUNT_TAG);
        }
        return data;
    }

    @Nonnull
    public ResourceLocation getGunId() {
        return Objects.requireNonNullElse(this.gunId, DEFAULT);
    }

    public void setGunId(@Nullable ResourceLocation gunId) {
        this.gunId = gunId;
    }

    public FireMode getFireMode() {
        return fireMode;
    }

    public void setFireMode(FireMode fireMode) {
        this.fireMode = fireMode;
    }

    public int getMaxAmmoCount() {
        return maxAmmoCount;
    }

    public void setMaxAmmoCount(int maxAmmoCount) {
        this.maxAmmoCount = maxAmmoCount;
    }

    public int getCurrentAmmoCount() {
        return currentAmmoCount;
    }

    public void setCurrentAmmoCount(int ammoCount) {
        this.currentAmmoCount = Math.min(ammoCount, this.maxAmmoCount);
    }

    public void reduceCurrentAmmoCount() {
        this.currentAmmoCount = Math.max(this.currentAmmoCount - 1, 0);
    }
}
