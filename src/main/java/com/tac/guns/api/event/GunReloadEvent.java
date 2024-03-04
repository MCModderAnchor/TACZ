package com.tac.guns.api.event;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.LogicalSide;

/**
 * 生物开始更换枪械弹药时触发的事件。
 */
@Cancelable
public class GunReloadEvent extends Event {
    private final LivingEntity entity;
    private final ItemStack gunItemStack;
    private final LogicalSide logicalSide;

    public GunReloadEvent(LivingEntity entity, ItemStack gunItemStack, LogicalSide side) {
        this.entity = entity;
        this.gunItemStack = gunItemStack;
        this.logicalSide = side;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public ItemStack getGunItemStack() {
        return gunItemStack;
    }

    public LogicalSide getLogicalSide() {
        return logicalSide;
    }
}
