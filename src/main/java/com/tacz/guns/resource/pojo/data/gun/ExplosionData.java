package com.tacz.guns.resource.pojo.data.gun;

import com.google.gson.annotations.SerializedName;

public class ExplosionData {
    @SerializedName("explode")
    private boolean explode;

    @SerializedName("radius")
    private float radius;

    @SerializedName("damage")
    private float damage;

    @SerializedName("knockback")
    private boolean knockback;

    @SerializedName("destroy_block")
    private boolean destroyBlock;

    /**
     * 无论是否触碰实体或者方块，默认延迟 30 秒就爆炸
     */
    @SerializedName("delay")
    private int delay;

    public ExplosionData(boolean explode, float radius, float damage, boolean knockback, int delay, boolean destroyBlock) {
        this.explode = explode;
        this.radius = radius;
        this.damage = damage;
        this.knockback = knockback;
        this.delay = delay;
        this.destroyBlock = destroyBlock;
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

    public boolean isDestroyBlock() {
        return destroyBlock;
    }

    public int getDelay() {
        return delay;
    }
}
