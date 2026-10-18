package com.tacz.guns.compat.kubejs.events;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.api.client.event.BeforeRenderHandEvent;
import com.tacz.guns.api.event.common.*;
import com.tacz.guns.api.event.server.AmmoHitBlockEvent;
import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.LogicalSide;

import javax.annotation.Nullable;

public interface TimelessForgeEventWrappers {
    interface ForgeEventWrapper<E extends Event> {
        E getForgeEvent();
    }

    //Common Events
    interface AttachmentPropertyWrapper extends ForgeEventWrapper<AttachmentPropertyEvent> {
        default ItemStack getGunItem() {
            return getForgeEvent().getGunItem();
        }

        default AttachmentCacheProperty getCacheProperty() {
            return getForgeEvent().getCacheProperty();
        }
    }

    interface EntityHurtByGunWrapper<E extends EntityHurtByGunEvent> extends ForgeEventWrapper<E> {
        default Entity getBullet() {
            return getForgeEvent().getBullet();
        }

        @Nullable
        default Entity getHurtEntity() {
            return getForgeEvent().getHurtEntity();
        }

        @Nullable
        default LivingEntity getAttacker() {
            return getForgeEvent().getAttacker();
        }

        default ResourceLocation getGunId() {
            return getForgeEvent().getGunId();
        }

        default float getBaseAmount() {
            return getForgeEvent().getBaseAmount();
        }

        default DamageSource getDamageSource(GunDamageSourcePart part) {
            return getForgeEvent().getDamageSource(part);
        }

        default float getHeadshotMultiplier() {
            return getForgeEvent().getHeadshotMultiplier();
        }

        default boolean isHeadShot() {
            return getForgeEvent().isHeadShot();
        }

        default LogicalSide getLogicalSide() {
            return getForgeEvent().getLogicalSide();
        }
    }

    interface EntityHurtByGunPreWrapper extends EntityHurtByGunWrapper<EntityHurtByGunEvent.Pre> {
        default void setHurtEntity(@Nullable Entity hurtEntity) {
            getForgeEvent().setHurtEntity(hurtEntity);
        }

        default void setAttacker(@Nullable LivingEntity attacker) {
            getForgeEvent().setAttacker(attacker);
        }

        default void setGunId(ResourceLocation gunId) {
            getForgeEvent().setGunId(gunId);
        }

        default void setBaseAmount(float baseAmount) {
            getForgeEvent().setBaseAmount(baseAmount);
        }

        default void setDamageSource(GunDamageSourcePart part, DamageSource value) {
            getForgeEvent().setDamageSource(part, value);
        }

        default void setHeadshot(boolean headshot) {
            getForgeEvent().setHeadshot(headshot);
        }

        default void setHeadshotMultiplier(float headshotMultiplier) {
            getForgeEvent().setHeadshotMultiplier(headshotMultiplier);
        }
    }

    interface EntityKillByGunWrapper extends ForgeEventWrapper<EntityKillByGunEvent> {
        default Entity getBullet() {
            return getForgeEvent().getBullet();
        }

        @Nullable
        default LivingEntity getKilledEntity() {
            return getForgeEvent().getKilledEntity();
        }

        @Nullable
        default LivingEntity getAttacker() {
            return getForgeEvent().getAttacker();
        }

        default ResourceLocation getGunId() {
            return getForgeEvent().getGunId();
        }

        default float getBaseDamage() {
            return getForgeEvent().getBaseDamage();
        }

        default DamageSource getDamageSource(GunDamageSourcePart part) {
            return getForgeEvent().getDamageSource(part);
        }

        default boolean isHeadShot() {
            return getForgeEvent().isHeadShot();
        }

        default float getHeadshotMultiplier() {
            return getForgeEvent().getHeadshotMultiplier();
        }

        default LogicalSide getLogicalSide() {
            return getForgeEvent().getLogicalSide();
        }

        default ResourceLocation getGunDisplayId() {
            return getForgeEvent().getGunDisplayId();
        }
    }

    interface GunDrawWrapper extends ForgeEventWrapper<GunDrawEvent> {
        default LivingEntity getEntity() {
            return getForgeEvent().getEntity();
        }

        default ItemStack getPreviousGunItem() {
            return getForgeEvent().getPreviousGunItem();
        }

        default ItemStack getCurrentGunItem() {
            return getForgeEvent().getCurrentGunItem();
        }

        default LogicalSide getLogicalSide() {
            return getForgeEvent().getLogicalSide();
        }
    }

    interface GunFinishReloadWrapper extends ForgeEventWrapper<GunFinishReloadEvent> {
        default LivingEntity getEntity() {
            return getForgeEvent().getEntity();
        }

        default ItemStack getGunItemStack() {
            return getForgeEvent().getGunItemStack();
        }

        default LogicalSide getLogicalSide() {
            return getForgeEvent().getLogicalSide();
        }
    }

    interface GunFireWrapper extends ForgeEventWrapper<GunFireEvent> {
        default LivingEntity getShooter() {
            return getForgeEvent().getShooter();
        }

        default ItemStack getGunItemStack() {
            return getForgeEvent().getGunItemStack();
        }

        default LogicalSide getLogicalSide() {
            return getForgeEvent().getLogicalSide();
        }
    }

    interface GunFirePostWrapper extends ForgeEventWrapper<GunFirePostEvent> {
        default LivingEntity getShooter() {
            return getForgeEvent().getShooter();
        }

        default ItemStack getGunItemStack() {
            return getForgeEvent().getGunItemStack();
        }

        default LogicalSide getLogicalSide() {
            return getForgeEvent().getLogicalSide();
        }
    }

    interface GunFireSelectWrapper extends ForgeEventWrapper<GunFireSelectEvent> {
        default LivingEntity getShooter() {
            return getForgeEvent().getShooter();
        }

        default ItemStack getGunItemStack() {
            return getForgeEvent().getGunItemStack();
        }

        default LogicalSide getLogicalSide() {
            return getForgeEvent().getLogicalSide();
        }
    }

    interface GunMeleeWrapper extends ForgeEventWrapper<GunMeleeEvent> {
        default LivingEntity getShooter() {
            return getForgeEvent().getShooter();
        }

        default ItemStack getGunItemStack() {
            return getForgeEvent().getGunItemStack();
        }

        default LogicalSide getLogicalSide() {
            return getForgeEvent().getLogicalSide();
        }
    }

    interface GunReloadWrapper extends ForgeEventWrapper<GunReloadEvent> {
        default LivingEntity getEntity() {
            return getForgeEvent().getEntity();
        }

        default ItemStack getGunItemStack() {
            return getForgeEvent().getGunItemStack();
        }

        default LogicalSide getLogicalSide() {
            return getForgeEvent().getLogicalSide();
        }
    }

    interface GunShootWrapper extends ForgeEventWrapper<GunShootEvent> {
        default LivingEntity getShooter() {
            return getForgeEvent().getShooter();
        }

        default ItemStack getGunItemStack() {
            return getForgeEvent().getGunItemStack();
        }

        default LogicalSide getLogicalSide() {
            return getForgeEvent().getLogicalSide();
        }
    }

    //Server Events
    interface AmmoHitBlockWrapper extends ForgeEventWrapper<AmmoHitBlockEvent> {
        default Level getLevel() {
            return getForgeEvent().getLevel();
        }

        default BlockHitResult getHitResult() {
            return getForgeEvent().getHitResult();
        }

        default BlockState getState() {
            return getForgeEvent().getState();
        }

        default EntityKineticBullet getAmmo() {
            return getForgeEvent().getAmmo();
        }
    }

    //Client Events
    interface BeforeRenderHandWrapper extends ForgeEventWrapper<BeforeRenderHandEvent> {
        default PoseStack getPoseStack() {
            return getForgeEvent().getPoseStack();
        }
    }
}
