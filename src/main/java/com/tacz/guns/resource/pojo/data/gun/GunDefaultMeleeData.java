package com.tacz.guns.resource.pojo.data.gun;

import com.google.gson.annotations.SerializedName;
import net.minecraft.world.phys.Vec3;

public class GunDefaultMeleeData {
    @SerializedName("animation_type")
    private String animationType = "melee_push";

    @SerializedName("range")
    private Vec3 range = new Vec3(2, 1, 1);

    @SerializedName("damage")
    private float damage = 0f;

    @SerializedName("knockback")
    private float knockback = 0.2f;

    @SerializedName("prep")
    private float prepTime = 0.1f;

    public String getAnimationType() {
        return animationType;
    }

    public Vec3 getRange() {
        return range;
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
