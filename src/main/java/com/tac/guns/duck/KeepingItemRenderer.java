package com.tac.guns.duck;

import net.minecraft.world.item.ItemStack;

public interface KeepingItemRenderer {
    void keep(ItemStack itemStack, long timeMs);

    ItemStack getCurrentItem();
}
