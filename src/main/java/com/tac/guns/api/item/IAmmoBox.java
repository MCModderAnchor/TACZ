package com.tac.guns.api.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public interface IAmmoBox {
    ResourceLocation getAmmoId(ItemStack ammoBox);

    int getAmmoCount(ItemStack ammoBox);

    void setAmmoId(ItemStack ammoBox, ResourceLocation ammoId);

    void setAmmoCount(ItemStack ammoBox, int count);

    boolean isAmmoBoxOfGun(ItemStack gun, ItemStack ammoBox);
}
