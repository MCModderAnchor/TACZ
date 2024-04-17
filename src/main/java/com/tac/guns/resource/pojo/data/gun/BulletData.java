package com.tac.guns.resource.pojo.data.gun;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Nullable;

public class BulletData {
    @SerializedName("life")
    private float lifeSecond = 10f;

    @SerializedName("damage")
    private float damageAmount = 5;

    @SerializedName("extra_damage")
    private @Nullable ExtraDamage extraDamage = null;

    @SerializedName("speed")
    private float speed = 5;

    @SerializedName("gravity")
    private float gravity = 0;

    @SerializedName("knockback")
    private float knockback = 0;

    @SerializedName("friction")
    private float friction = 0.01f;

    @SerializedName("ignite")
    private boolean hasIgnite = false;

    @SerializedName("explosion")
    private @Nullable ExplosionData explosionData;

    public float getLifeSecond() {
        return lifeSecond;
    }

    public float getDamageAmount() {
        return damageAmount;
    }

    @Nullable
    public ExtraDamage getExtraDamage() {
        return extraDamage;
    }

    public float getSpeed() {
        return speed;
    }

    public float getGravity() {
        return gravity;
    }

    public float getKnockback() {
        return knockback;
    }

    public float getFriction() {
        return friction;
    }

    public boolean isHasIgnite() {
        return hasIgnite;
    }

    @Nullable
    public ExplosionData getExplosionData() {
        return explosionData;
    }
}
