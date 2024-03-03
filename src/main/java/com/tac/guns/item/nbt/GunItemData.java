package com.tac.guns.item.nbt;

import com.tac.guns.GunMod;
import com.tac.guns.api.gun.FireMode;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class GunItemData {
    public static final ResourceLocation DEFAULT = new ResourceLocation(GunMod.MOD_ID, "ak47");
    public static final ResourceLocation DEFAULT_DISPLAY = new ResourceLocation(GunMod.MOD_ID, "ak47_display");
    public static final String GUN_ID_TAG = "GunId";
    public static final String GUN_FIRE_MODE_TAG = "GunFireMode";
    @Nullable
    private ResourceLocation gunId = null;
    private FireMode fireMode = FireMode.SEMI;

    public static void serialization(@Nonnull CompoundTag nbt, @Nonnull GunItemData data) {
        if (data.gunId != null) {
            nbt.putString(GUN_ID_TAG, data.gunId.toString());
        }
        nbt.putString(GUN_FIRE_MODE_TAG, data.fireMode.name());
    }

    public static @Nonnull GunItemData deserialization(@Nonnull CompoundTag nbt) {
        GunItemData data = new GunItemData();
        if (nbt.contains(GUN_ID_TAG, Tag.TAG_STRING)) {
            data.gunId = ResourceLocation.tryParse(nbt.getString(GUN_ID_TAG));
        }
        if (nbt.contains(GUN_FIRE_MODE_TAG, Tag.TAG_STRING)) {
            data.fireMode = FireMode.valueOf(nbt.getString(GUN_FIRE_MODE_TAG));
        }
        return data;
    }

    @Nonnull
    public ResourceLocation getGunId() {
        if (this.gunId == null) {
            return DEFAULT;
        }
        return gunId;
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
}
