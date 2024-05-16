package com.tacz.guns.resource.pojo.data.gun;

import com.google.gson.annotations.SerializedName;

public class ExplosionData {
    @SerializedName("radius")
    private float radius = 5;

    @SerializedName("damage")
    private float damage = 5;

    @SerializedName("knockback")
    private boolean knockback = false;

    public float getRadius() {
        return radius;
    }

    public float getDamage() {
        return damage;
    }

    public boolean isKnockback() {
        return knockback;
    }
}
