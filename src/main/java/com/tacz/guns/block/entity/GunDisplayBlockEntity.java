package com.tacz.guns.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/** 展示方块实体的共同契约；每个展示面按网格大小的平方连续编号。 */
public interface GunDisplayBlockEntity {
    ItemStack getItem(int slot);

    int getRotation(int slot);

    DisplayGunSlot getDisplaySlot(int slot);

    int getGridSize();

    Direction getSlotFacing(int slot);

    /** 展示物品是否使用满亮度。 */
    boolean isDisplayGlowing();

    BlockPos getBlockPos();

    @Nullable
    Level getLevel();

    boolean isRemoved();
}
