package com.tacz.guns.util;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public final class AmmoStorageHelper {
    private AmmoStorageHelper() {
    }

    public static boolean isValidAmmo(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof IAmmo ammo)) {
            return false;
        }
        return TimelessAPI.getCommonAmmoIndex(ammo.getAmmoId(stack)).isPresent();
    }

    @Nullable
    public static ResourceLocation getAmmoId(ItemStack stack) {
        if (stack.getItem() instanceof IAmmo ammo) {
            return ammo.getAmmoId(stack);
        }
        return null;
    }

    public static int getSort(ItemStack stack) {
        ResourceLocation id = getAmmoId(stack);
        if (id == null) {
            return Integer.MAX_VALUE;
        }
        return TimelessAPI.getCommonAmmoIndex(id).map(index -> index.getSort()).orElse(Integer.MAX_VALUE);
    }

    public static int getStackSize(ItemStack stack) {
        ResourceLocation id = getAmmoId(stack);
        if (id == null) {
            return stack.getMaxStackSize();
        }
        return TimelessAPI.getCommonAmmoIndex(id).map(index -> index.getStackSize()).orElse(stack.getMaxStackSize());
    }

    public static ItemStack createAmmoStack(ResourceLocation ammoId, int count) {
        return AmmoItemBuilder.create().setId(ammoId).setCount(count).build();
    }
}
