package com.tac.guns.api.event.common;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.LogicalSide;

import javax.annotation.Nullable;

public class LivingKillByGunEvent extends Event {
    private final @Nullable LivingEntity killedEntity;
    private final @Nullable LivingEntity attacker;
    private final ResourceLocation gunId;
    private final boolean isHeadShot;
    private final LogicalSide logicalSide;

    public LivingKillByGunEvent(@Nullable LivingEntity hurtEntity, @Nullable LivingEntity attacker, ResourceLocation gunId, boolean isHeadShot, LogicalSide logicalSide) {
        this.killedEntity = hurtEntity;
        this.attacker = attacker;
        this.gunId = gunId;
        this.isHeadShot = isHeadShot;
        this.logicalSide = logicalSide;
    }

    @Nullable
    public LivingEntity getKilledEntity() {
        return killedEntity;
    }

    @Nullable
    public LivingEntity getAttacker() {
        return attacker;
    }

    public ResourceLocation getGunId() {
        return gunId;
    }

    public boolean isHeadShot() {
        return isHeadShot;
    }

    public LogicalSide getLogicalSide() {
        return logicalSide;
    }
}