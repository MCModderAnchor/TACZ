package com.tacz.guns.api.event.common;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.LogicalSide;

import javax.annotation.Nullable;

/**
 * 生物被枪械子弹伤害时触发的事件
 */
public class LivingHurtByGunEvent extends Event {
    private final @Nullable LivingEntity hurtEntity;
    private final @Nullable LivingEntity attacker;
    private final ResourceLocation gunId;
    private final float amount;
    private final boolean isHeadShot;
    private final LogicalSide logicalSide;

    public LivingHurtByGunEvent(@Nullable LivingEntity hurtEntity, @Nullable LivingEntity attacker, ResourceLocation gunId, float amount, boolean isHeadShot, LogicalSide logicalSide) {
        this.hurtEntity = hurtEntity;
        this.attacker = attacker;
        this.gunId = gunId;
        this.amount = amount;
        this.isHeadShot = isHeadShot;
        this.logicalSide = logicalSide;
    }

    @Nullable
    public LivingEntity getHurtEntity() {
        return hurtEntity;
    }

    @Nullable
    public LivingEntity getAttacker() {
        return attacker;
    }

    public ResourceLocation getGunId() {
        return gunId;
    }

    public float getAmount() {
        return amount;
    }

    public boolean isHeadShot() {
        return isHeadShot;
    }

    public LogicalSide getLogicalSide() {
        return logicalSide;
    }
}
