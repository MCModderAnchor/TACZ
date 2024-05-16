package com.tacz.guns.crafting;

import net.minecraft.world.item.ItemStack;

public class GunSmithTableResult {
    public static final String GUN = "gun";
    public static final String AMMO = "ammo";
    public static final String ATTACHMENT = "attachment";

    private final ItemStack result;
    private final String group;

    public GunSmithTableResult(ItemStack result, String group) {
        this.result = result;
        this.group = group;
    }

    public ItemStack getResult() {
        return result;
    }

    public String getGroup() {
        return group;
    }
}
