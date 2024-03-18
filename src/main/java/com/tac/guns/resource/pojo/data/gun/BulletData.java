package com.tac.guns.resource.pojo.data.gun;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Nullable;

public class BulletData {
    @SerializedName("life")
    private float lifeSecond = 10f;

    @SerializedName("hurt")
    private float hurtAmount = 5;

    @SerializedName("speed")
    private float speed = 5;

    @SerializedName("gravity")
    private float gravity = 0;

    @SerializedName("knockback")
    private float knockback = 0;

    @SerializedName("explosion")
    private @Nullable ExplosionData explosionData;

    public float getLifeSecond() {
        return lifeSecond;
    }

    public float getHurtAmount() {
        return hurtAmount;
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

    @Nullable
    public ExplosionData getExplosionData() {
        return explosionData;
    }
}
