package com.tac.guns.inventory.tooltip;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

public class AmmoBoxTooltip implements TooltipComponent {
    private final ItemStack ammo;
    private final int count;

    public AmmoBoxTooltip(ItemStack ammo, int count) {
        this.ammo = ammo;
        this.count = count;
    }

    public ItemStack getAmmo() {
        return ammo;
    }

    public int getCount() {
        return count;
    }
}
