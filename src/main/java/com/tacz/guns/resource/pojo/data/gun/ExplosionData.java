package com.tacz.guns.resource.pojo.data.gun;

import com.google.gson.annotations.SerializedName;

public class ExplosionData {
    @SerializedName("explode")
    private boolean explode = true;

    @SerializedName("radius")
    private float radius = 0.5f;

    @SerializedName("damage")
    private float damage = 2;

    @SerializedName("knockback")
    private boolean knockback = false;

    /**
     * 无论是否触碰实体或者方块，默认延迟 30 秒就爆炸
     */
    @SerializedName("delay")
    private int delay = 30;

    public ExplosionData() {
    }

    public ExplosionData(boolean explode, float radius, float damage, boolean knockback, int delay) {
        this.explode = explode;
        this.radius = radius;
        this.damage = damage;
        this.knockback = knockback;
        this.delay = delay;
    }

    public boolean isExplode() {
        return explode;
    }

    public float getRadius() {
        return radius;
    }

    public float getDamage() {
        return damage;
    }

    public boolean isKnockback() {
        return knockback;
    }

    public int getDelay() {
        return delay;
    }
}
