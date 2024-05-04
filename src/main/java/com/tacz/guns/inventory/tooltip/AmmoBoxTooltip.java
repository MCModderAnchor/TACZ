package com.tacz.guns.inventory.tooltip;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

public class AmmoBoxTooltip implements TooltipComponent {
    private final ItemStack ammoBox;
    private final ItemStack ammo;
    private final int count;

    public AmmoBoxTooltip(ItemStack ammoBox, ItemStack ammo, int count) {
        this.ammoBox = ammoBox;
        this.ammo = ammo;
        this.count = count;
    }

    public ItemStack getAmmoBox() {
        return ammoBox;
    }

    public ItemStack getAmmo() {
        return ammo;
    }

    public int getCount() {
        return count;
    }
}
