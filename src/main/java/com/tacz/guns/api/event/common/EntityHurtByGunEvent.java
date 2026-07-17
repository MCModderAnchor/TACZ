package com.tacz.guns.api.event.common;

import com.tacz.guns.util.BulletDamageContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.LogicalSide;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.ApiStatus.Obsolete;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * 生物被枪械子弹伤害时触发的事件
 */
public class EntityHurtByGunEvent extends Event implements KubeJSGunEventPoster<EntityHurtByGunEvent> {
    protected final Entity bullet;
    protected @Nullable Entity hurtEntity;
    protected @Nullable LivingEntity attacker;
    protected ResourceLocation gunId;
    protected ResourceLocation gunDisplayId;
    protected float baseAmount;
    protected DamageSource nonApPartDamageSource;
    protected DamageSource apPartDamageSource;
    protected boolean isHeadShot;
    protected float headshotMultiplier;
    protected final LogicalSide logicalSide;

    @ApiStatus.Internal
    protected EntityHurtByGunEvent(Entity bullet, @Nullable Entity hurtEntity, @Nullable LivingEntity attacker,
                                   ResourceLocation gunId, ResourceLocation gunDisplayId,
                                   float baseAmount, @Nullable Pair<DamageSource, DamageSource> sources, boolean isHeadShot,
                                   float headshotMultiplier, LogicalSide logicalSide) {
        this.bullet = bullet;
        this.hurtEntity = hurtEntity;
        this.attacker = attacker;
        this.gunId = gunId;
        this.baseAmount = baseAmount;
        this.nonApPartDamageSource = Optional.ofNullable(sources).map(Pair::getLeft).orElse(null);
        this.apPartDamageSource = Optional.ofNullable(sources).map(Pair::getRight).orElse(null);
        this.isHeadShot = isHeadShot;
        this.headshotMultiplier = headshotMultiplier;
        this.logicalSide = logicalSide;
    }

    /**
     * 实体受到枪击，伤害判定前触发的事件，可以设置枪击的伤害属性
     */
    @Cancelable
    public static class Pre extends EntityHurtByGunEvent {
        @ApiStatus.Internal
        public Pre(Entity bullet, @Nullable Entity hurtEntity, @Nullable LivingEntity attacker,
                   ResourceLocation gunId, ResourceLocation gunDisplayId,
                   float amount, @Nullable Pair<DamageSource, DamageSource> sources,
                   boolean isHeadShot, float headshotMultiplier, LogicalSide logicalSide) {
            super(bullet, hurtEntity, attacker, gunId, gunDisplayId, amount, sources, isHeadShot, headshotMultiplier, logicalSide);
            this.headshotMultiplier = headshotMultiplier;
            postEventToKubeJS(this);
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

        public final void setDamageSource(GunDamageSourcePart part, DamageSource value) {
            if (logicalSide.isClient()) {
                throw new UnsupportedOperationException("DamageSource about gun hit is not available on client side!");
            }
            if (part == GunDamageSourcePart.ARMOR_PIERCING) {
                apPartDamageSource = value;
            } else {
                nonApPartDamageSource = value;
            }
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
        @ApiStatus.Internal
        public Post(Entity bullet, @Nullable Entity hurtEntity, @Nullable LivingEntity attacker,
                    ResourceLocation gunId, ResourceLocation gunDisplayId,
                    float amount, @Nullable Pair<DamageSource, DamageSource> sources,
                    boolean isHeadShot, float headshotMultiplier, LogicalSide logicalSide) {
            super(bullet, hurtEntity, attacker, gunId, gunDisplayId, amount, sources, isHeadShot, headshotMultiplier, logicalSide);
            postEventToKubeJS(this);
        }
    }

    public Entity getBullet() {
        return bullet;
    }

    @Nullable
    public Entity getHurtEntity() {
        return hurtEntity;
    }

    @Nullable
    public LivingEntity getAttacker() {
        return BulletDamageContext.isShooterHidden() ? null : attacker;
    }

    public ResourceLocation getGunId() {
        return gunId;
    }

    public ResourceLocation getGunDisplayId() {
        return gunDisplayId;
    }

    @Obsolete
    public float getAmount() {
        return baseAmount * headshotMultiplier;
    }

    public float getBaseAmount() {
        return baseAmount;
    }

    public DamageSource getDamageSource(GunDamageSourcePart part) {
        if (logicalSide.isClient()) {
            throw new UnsupportedOperationException("DamageSource about gun hit is not available on client side!");
        }
        DamageSource source = part == GunDamageSourcePart.ARMOR_PIERCING ? apPartDamageSource : nonApPartDamageSource;
        if (source != null && BulletDamageContext.isShooterHidden()) {
            return BulletDamageContext.withoutShooter(source);
        }
        return source;
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
