package com.tac.guns.item.nbt;

import com.tac.guns.GunMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class AmmoItemData {
    public static final ResourceLocation DEFAULT = new ResourceLocation(GunMod.MOD_ID, "762x39");
    public static final ResourceLocation DEFAULT_DISPLAY = new ResourceLocation(GunMod.MOD_ID, "762x39_display");

    public static final String AMMO_ID_TAG = "AmmoId";
    public static final String AMMO_STACK_TAG = "AmmoStack";

    @Nullable
    private ResourceLocation ammoId = null;
    private int ammoStack = 1;

    public static void serialization(@Nonnull CompoundTag nbt, @Nonnull AmmoItemData data) {
        if (data.ammoId != null) {
            nbt.putString(AMMO_ID_TAG, data.ammoId.toString());
        }
        nbt.putInt(AMMO_STACK_TAG, data.ammoStack);
    }

    public static @Nonnull AmmoItemData deserialization(@Nonnull CompoundTag nbt) {
        AmmoItemData data = new AmmoItemData();
        if (nbt.contains(AMMO_ID_TAG, Tag.TAG_STRING)) {
            data.ammoId = ResourceLocation.tryParse(nbt.getString(AMMO_ID_TAG));
        }
        if (nbt.contains(AMMO_STACK_TAG, Tag.TAG_INT)) {
            data.ammoStack = nbt.getInt(AMMO_STACK_TAG);
        }
        return data;
    }

    @Nonnull
    public ResourceLocation getAmmoId() {
        if (this.ammoId == null) {
            return DEFAULT;
        }
        return ammoId;
    }

    public int getAmmoStack() {
        return Math.max(ammoStack, 1);
    }

    public void setAmmoId(@Nullable ResourceLocation ammoId) {
        this.ammoId = ammoId;
    }

    public void setAmmoStack(int ammoStack) {
        this.ammoStack = ammoStack;
    }
}
