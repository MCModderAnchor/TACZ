package com.tacz.guns.resource.pojo.data.attachment;

import com.google.common.collect.Lists;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class MeleeData {
    @SerializedName("distance")
    private float distance = 1f;

    @SerializedName("range_angle")
    private float rangeAngle = 30f;

    @SerializedName("cooldown")
    private float cooldown = 0f;

    @SerializedName("damage")
    private float damage = 0f;

    @SerializedName("knockback")
    private float knockback = 0f;

    @SerializedName("prep")
    private float prepTime = 0.1f;

    @SerializedName("effects")
    private List<EffectData> effects = Lists.newArrayList();

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

    public List<EffectData> getEffects() {
        return effects;
    }
}
