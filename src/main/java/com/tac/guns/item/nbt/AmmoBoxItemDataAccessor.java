package com.tac.guns.item.nbt;

import com.tac.guns.api.TimelessAPI;
import com.tac.guns.api.item.IAmmoBox;
import com.tac.guns.api.item.IGun;
import com.tac.guns.resource.DefaultAssets;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public interface AmmoBoxItemDataAccessor extends IAmmoBox {
    String AMMO_ID_TAG = "AmmoId";
    String AMMO_COUNT_TAG = "AmmoCount";

    @Override
    default ResourceLocation getAmmoId(ItemStack ammoBox) {
        CompoundTag tag = ammoBox.getOrCreateTag();
        if (tag.contains(AMMO_ID_TAG, Tag.TAG_STRING)) {
            return new ResourceLocation(tag.getString(AMMO_ID_TAG));
        }
        return DefaultAssets.EMPTY_AMMO_ID;
    }

    @Override
    default void setAmmoId(ItemStack ammoBox, ResourceLocation ammoId) {
        CompoundTag tag = ammoBox.getOrCreateTag();
        tag.putString(AMMO_ID_TAG, ammoId.toString());
    }

    @Override
    default int getAmmoCount(ItemStack ammoBox) {
        CompoundTag tag = ammoBox.getOrCreateTag();
        if (tag.contains(AMMO_COUNT_TAG, Tag.TAG_INT)) {
            return tag.getInt(AMMO_COUNT_TAG);
        }
        return 0;
    }

    @Override
    default void setAmmoCount(ItemStack ammoBox, int count) {
        CompoundTag tag = ammoBox.getOrCreateTag();
        tag.putInt(AMMO_COUNT_TAG, count);
    }

    @Override
    default boolean isAmmoBoxOfGun(ItemStack gun, ItemStack ammoBox) {
        if (gun.getItem() instanceof IGun iGun && ammoBox.getItem() instanceof IAmmoBox iAmmoBox) {
            ResourceLocation ammoId = iAmmoBox.getAmmoId(ammoBox);
            if (ammoId.equals(DefaultAssets.EMPTY_AMMO_ID)) {
                return false;
            }
            ResourceLocation gunId = iGun.getGunId(gun);
            return TimelessAPI.getCommonGunIndex(gunId).map(gunIndex -> gunIndex.getGunData().getAmmoId().equals(ammoId)).orElse(false);
        }
        return false;
    }
}
