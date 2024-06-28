package com.tacz.guns.api.event.common;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.LogicalSide;

/**
 * 用枪近战时触发
 */
public class GunMeleeEvent extends Event {
    private final LivingEntity shooter;
    private final ItemStack gunItemStack;
    private final LogicalSide logicalSide;

    public GunMeleeEvent(LivingEntity shooter, ItemStack gunItemStack, LogicalSide side) {
        this.shooter = shooter;
        this.gunItemStack = gunItemStack;
        this.logicalSide = side;
    }

    @Override
    public boolean isCancelable() {
        return true;
    }

    public LivingEntity getShooter() {
        return shooter;
    }

    public ItemStack getGunItemStack() {
        return gunItemStack;
    }

    public LogicalSide getLogicalSide() {
        return logicalSide;
    }
}
