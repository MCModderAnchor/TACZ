package com.tacz.guns.api.event.common;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.LogicalSide;
import org.jetbrains.annotations.ApiStatus.Obsolete;

import javax.annotation.Nullable;

/**
 * 生物被枪械子弹伤害时触发的事件
 */
public class EntityHurtByGunEvent extends Event {
    protected @Nullable Entity hurtEntity;
    protected @Nullable LivingEntity attacker;
    protected ResourceLocation gunId;
    protected float baseAmount;
    protected boolean isHeadShot;
    protected float headshotMultiplier;
    private final LogicalSide logicalSide;

    protected EntityHurtByGunEvent(@Nullable Entity hurtEntity, @Nullable LivingEntity attacker, ResourceLocation gunId, float baseAmount, boolean isHeadShot, float headshotMultiplier, LogicalSide logicalSide) {
        this.hurtEntity = hurtEntity;
        this.attacker = attacker;
        this.gunId = gunId;
        this.baseAmount = baseAmount;
        this.isHeadShot = isHeadShot;
        this.headshotMultiplier = headshotMultiplier;
        this.logicalSide = logicalSide;
    }

    /**
     * 实体受到枪击，伤害判定前触发的事件，可以设置枪击的伤害属性
     */
    @Cancelable
    public static class Pre extends EntityHurtByGunEvent {
        public Pre(@Nullable Entity hurtEntity, @Nullable LivingEntity attacker, ResourceLocation gunId, float amount, boolean isHeadShot, float headshotMultiplier, LogicalSide logicalSide) {
            super(hurtEntity, attacker, gunId, amount, isHeadShot, headshotMultiplier, logicalSide);
            this.headshotMultiplier = headshotMultiplier;
        }

        public final void setHurtEntity(@Nullable Entity hurtEntity) {
            this.hurtEntity = hurtEntity;
        }

        public final void setAttacker(@Nullable LivingEntity attacker) {
            this.attacker = attacker;
        }

        public final void setGunId(ResourceLocation gunId) {
            this.gunId = gunId;
        }

        public final void setBaseAmount(float baseAmount) {
            this.baseAmount = baseAmount;
        }

        public final void setHeadshot(boolean headshot) {
            this.isHeadShot = headshot;
        }

        public final void setHeadshotMultiplier(float headshotMultiplier) {
            this.headshotMultiplier = headshotMultiplier;
        }
    }

    /**
     * 实体受到枪击，伤害判定结束但没有死亡后触发的事件
     * @see EntityKillByGunEvent 实体因枪击致死时触发的事件
     */
    public static class Post extends EntityHurtByGunEvent {
        public Post(@Nullable Entity hurtEntity, @Nullable LivingEntity attacker, ResourceLocation gunId, float amount, boolean isHeadShot, float headshotMultiplier, LogicalSide logicalSide) {
            super(hurtEntity, attacker, gunId, amount, isHeadShot, headshotMultiplier, logicalSide);
        }
    }

    @Nullable
    public Entity getHurtEntity() {
        return hurtEntity;
    }

    @Nullable
    public LivingEntity getAttacker() {
        return attacker;
    }

    public ResourceLocation getGunId() {
        return gunId;
    }

    @Obsolete
    public float getAmount() {
        return baseAmount * headshotMultiplier;
    }

    public float getBaseAmount() {
        return baseAmount;
    }

    public float getHeadshotMultiplier() {
        return headshotMultiplier;
    }

    public boolean isHeadShot() {
        return isHeadShot;
    }

    public LogicalSide getLogicalSide() {
        return logicalSide;
    }
}
