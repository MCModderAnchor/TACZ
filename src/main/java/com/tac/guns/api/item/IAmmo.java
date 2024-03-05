package com.tac.guns.api.item;

import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public interface IAmmo {
    /**
     * 该物品是否为弹药
     */
    static boolean isAmmo(@Nullable ItemStack stack) {
        if (stack == null) {
            return false;
        }
        return stack.getItem() instanceof IAmmo;
    }
}
