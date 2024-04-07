package com.tac.guns.api.entity;

import net.minecraft.world.entity.LivingEntity;

public interface KnockBackModifier {
    /**
     * LivingEntity 通过 Mixin 的方式实现了这个接口
     */
    static KnockBackModifier fromLivingEntity(LivingEntity entity) {
        return (KnockBackModifier) entity;
    }

    void resetKnockBackStrength();

    double getKnockBackStrength();

    void setKnockBackStrength(double strength);
}
