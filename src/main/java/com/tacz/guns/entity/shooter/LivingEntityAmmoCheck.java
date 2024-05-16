package com.tacz.guns.entity.shooter;

import com.tacz.guns.config.common.GunConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class LivingEntityAmmoCheck {
    private final LivingEntity shooter;

    public LivingEntityAmmoCheck(LivingEntity shooter) {
        this.shooter = shooter;
    }

    public boolean needCheckAmmo() {
        if (shooter instanceof Player player) {
            return !player.isCreative();
        }
        return true;
    }

    public boolean consumesAmmoOrNot() {
        if (shooter instanceof Player player) {
            return !player.isCreative() || GunConfig.CREATIVE_PLAYER_CONSUME_AMMO.get();
        }
        return true;
    }
}
