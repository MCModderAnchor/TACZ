package com.tac.guns.api.entity;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;

/**
 * 用于进行一些并非 {@link LivingEntity} 但是可被子弹击中的特殊实体的处理
 */
public interface ITargetEntity {
    /**
     * @param projectile 弹射物实体
     * @param result     击中实体的位置
     * @param source     伤害源类型
     * @param damage     伤害值
     * @return true, 如果实体被击中并且应该被移除, 否则应返回 false
     */
    boolean onProjectileHit(Entity projectile, EntityHitResult result, DamageSource source, float damage);
}
