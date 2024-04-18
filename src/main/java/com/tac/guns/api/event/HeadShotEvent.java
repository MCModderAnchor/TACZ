package com.tac.guns.api.event;

import com.tac.guns.entity.EntityBullet;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.eventbus.api.Event;

import javax.annotation.Nullable;

public class HeadShotEvent extends Event {
    private final EntityBullet ammo;
    private final @Nullable Entity shooter;
    private final float damage;

    public HeadShotEvent(EntityBullet ammo, @Nullable Entity shooter, float damage) {
        this.ammo = ammo;
        this.shooter = shooter;
        this.damage = damage;
    }

    public EntityBullet getAmmo() {
        return ammo;
    }

    @Nullable
    public Entity getShooter() {
        return shooter;
    }

    public float getDamage() {
        return damage;
    }
}
