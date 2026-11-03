package com.tacz.guns.inventory;

import net.minecraft.world.item.ItemStack;

/** A menu-local identity and display snapshot, never a mutable inventory slot. */
public record AmmoStorageEntry(int id, ItemStack sample, long count, long remainingCapacity) {
    public AmmoStorageEntry {
        sample = sample.copyWithCount(1);
    }
}
