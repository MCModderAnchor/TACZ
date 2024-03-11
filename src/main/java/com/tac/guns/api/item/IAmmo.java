package com.tac.guns.api.item;

import net.minecraft.resources.ResourceLocation;
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

    /**
     * 获取弹药 ID
     *
     * @param ammo 输入物品
     * @return 弹药 ID
     */
    ResourceLocation getAmmoId(ItemStack ammo);

    void setAmmoId(ItemStack gun, @Nullable ResourceLocation ammoId);

    boolean isAmmoOfGun(ItemStack gun, ItemStack ammo);
}
