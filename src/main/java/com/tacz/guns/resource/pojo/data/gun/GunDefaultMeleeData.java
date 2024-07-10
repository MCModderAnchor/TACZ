package com.tacz.guns.resource.pojo.data.gun;

import com.google.gson.annotations.SerializedName;

public class GunDefaultMeleeData {
    @SerializedName("animation_type")
    private String animationType = "melee_push";

    @SerializedName("distance")
    private float distance = 1f;

    @SerializedName("range_angle")
    private float rangeAngle = 30f;

    @SerializedName("cooldown")
    private float cooldown = 0f;

    @SerializedName("damage")
    private float damage = 0f;

    @SerializedName("knockback")
    private float knockback = 0.2f;

    @SerializedName("prep")
    private float prepTime = 0.1f;

    public String getAnimationType() {
        return animationType;
    }

    public float getDistance() {
        return distance;
    }

    public float getRangeAngle() {
        return rangeAngle;
    }

    public float getCooldown() {
        return cooldown;
    }

    public float getDamage() {
        return damage;
    }

    public float getKnockback() {
        return knockback;
    }

    public float getPrepTime() {
        return prepTime;
    }
}
