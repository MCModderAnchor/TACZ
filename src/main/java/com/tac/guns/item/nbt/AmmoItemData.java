package com.tac.guns.item.nbt;

import com.tac.guns.GunMod;
import com.tac.guns.api.item.IAmmo;
import com.tac.guns.api.item.IGun;
import com.tac.guns.resource.CommonGunPackLoader;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public interface AmmoItemData extends IAmmo {
    ResourceLocation DEFAULT = new ResourceLocation(GunMod.MOD_ID, "762x39");
    ResourceLocation DEFAULT_DISPLAY = new ResourceLocation(GunMod.MOD_ID, "762x39_display");

    String AMMO_ID_TAG = "AmmoId";
    String AMMO_STACK_TAG = "AmmoStack";

    @Override
    @Nonnull
    default ResourceLocation getAmmoId(ItemStack ammo) {
        CompoundTag nbt = ammo.getOrCreateTag();
        if (nbt.contains(AMMO_ID_TAG, Tag.TAG_STRING)) {
            ResourceLocation gunId = ResourceLocation.tryParse(nbt.getString(AMMO_ID_TAG));
            return Objects.requireNonNullElse(gunId, DEFAULT);
        }
        return DEFAULT;
    }

    @Override
    default void setAmmoId(ItemStack ammo, @Nullable ResourceLocation ammoId) {
        CompoundTag nbt = ammo.getOrCreateTag();
        if (ammoId != null) {
            nbt.putString(AMMO_ID_TAG, ammoId.toString());
            return;
        }
        nbt.putString(AMMO_ID_TAG, DEFAULT.toString());
    }

    default int getAmmoStack(ItemStack ammo) {
        CompoundTag nbt = ammo.getOrCreateTag();
        if (nbt.contains(AMMO_STACK_TAG, Tag.TAG_INT)) {
            return Math.max(nbt.getInt(AMMO_STACK_TAG), 1);
        }
        return 1;
    }

    default void setAmmoStack(ItemStack ammo, int ammoStackCount) {
        CompoundTag nbt = ammo.getOrCreateTag();
        nbt.putInt(AMMO_STACK_TAG, Math.max(ammoStackCount, 1));
    }

    default boolean isAmmoOfGun(ItemStack gun, ItemStack ammo) {
        if (gun.getItem() instanceof IGun iGun && ammo.getItem() instanceof IAmmo iAmmo) {
            ResourceLocation gunId = iGun.getGunId(gun);
            ResourceLocation ammoId = iAmmo.getAmmoId(ammo);
            return CommonGunPackLoader.getGunIndex(gunId).map(gunIndex -> gunIndex.getGunData().getAmmoId().equals(ammoId)).orElse(false);
        }
        return false;
    }
}
