package com.tac.guns.item.nbt;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class GunItemData {
    public static final String GUN_ID_TAG = "GunId";
    private String gunId;

    public GunItemData(){
        gunId = null;
    }

    public static void serialization(@Nonnull CompoundTag nbt, @Nonnull GunItemData data){
        if(data.gunId != null) nbt.putString(GUN_ID_TAG, data.gunId);
    }

    public static @Nonnull GunItemData deserialization(@Nonnull CompoundTag nbt) {
        GunItemData data = new GunItemData();
        if (nbt.contains(GUN_ID_TAG, Tag.TAG_STRING)) {
            data.gunId = nbt.getString(GUN_ID_TAG);
        }
        return data;
    }

    public void setGunId(String gunId){
        this.gunId = gunId;
    }

    public @Nullable String getGunId() {
        return gunId;
    }
}
