package com.tacz.guns.resource.pojo.data.gun;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Nullable;

public class BulletData {
    @SerializedName("life")
    private float lifeSecond = 10f;

    @SerializedName("bullet_amount")
    private int bulletAmount = 1;

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

    @SerializedName("pierce")
    private int pierce = 1;

    @SerializedName("ignite")
    private Ignite ignite = new Ignite(false);

    @SerializedName("ignite_entity_time")
    private int igniteEntityTime = 2;

    @SerializedName("tracer_count_interval")
    private int tracerCountInterval = -1;

    @SerializedName("explosion")
    private @Nullable ExplosionData explosionData;

    public float getLifeSecond() {
        return lifeSecond;
    }

    public int getBulletAmount() {
        return bulletAmount;
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

    public int getPierce() {
        return pierce;
    }

    public Ignite getIgnite() {
        return ignite;
    }

    public int getIgniteEntityTime() {
        return igniteEntityTime;
    }

    public boolean hasTracerAmmo() {
        return this.tracerCountInterval >= 0;
    }

    public int getTracerCountInterval() {
        return tracerCountInterval;
    }

    @Nullable
    public ExplosionData getExplosionData() {
        return explosionData;
    }
}
