package com.tacz.guns.resource.pojo.data.gun;

import com.google.gson.annotations.SerializedName;

public class ExplosionData {
    @SerializedName("explode")
    private boolean explode = true;

    @SerializedName("radius")
    private float radius = 5;

    @SerializedName("damage")
    private float damage = 5;

    @SerializedName("knockback")
    private boolean knockback = false;

    @SerializedName("delay")
    private int delay = Integer.MAX_VALUE;

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
