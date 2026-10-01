package com.tacz.guns.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public enum GunTooltipPart {
    DESCRIPTION,
    AMMO_INFO,
    BASE_INFO,
    EXTRA_DAMAGE_INFO,
    UPGRADES_TIP,
    PACK_INFO;

    private final int mask = 1 << this.ordinal();

    public int getMask() {
        return this.mask;
    }

    @SuppressWarnings("deprecation")
    public static int getHideFlags(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).getUnsafe();
        if (tag.contains("HideFlags", Tag.TAG_ANY_NUMERIC)) {
            return tag.getInt("HideFlags");
        }
        return 0;
    }

    public static void setHideFlags(ItemStack stack, int mask) {
        if ((mask & 1) == 1) stack.set(DataComponents.HIDE_TOOLTIP, Unit.INSTANCE);
        if ((mask & 2) == 2) stack.set(DataComponents.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
    }
}
