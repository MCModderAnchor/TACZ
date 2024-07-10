package com.tacz.guns.api.event.common;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.LogicalSide;

/**
 * 生物开始更换枪械弹药时触发的事件。
 */
public class GunDrawEvent extends Event {
    private final LivingEntity entity;
    private final ItemStack previousGunItem;
    private final ItemStack currentGunItem;
    private final LogicalSide logicalSide;

    public GunDrawEvent(LivingEntity entity, ItemStack previousGunItem, ItemStack currentGunItem, LogicalSide side) {
        this.entity = entity;
        this.previousGunItem = previousGunItem;
        this.currentGunItem = currentGunItem;
        this.logicalSide = side;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public ItemStack getPreviousGunItem() {
        return previousGunItem;
    }

    public ItemStack getCurrentGunItem() {
        return currentGunItem;
    }

    public LogicalSide getLogicalSide() {
        return logicalSide;
    }
}
