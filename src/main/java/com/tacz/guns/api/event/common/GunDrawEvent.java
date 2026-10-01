package com.tacz.guns.api.event.common;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.LogicalSide;

/**
 * 生物开始切换枪械时触发的事件
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
