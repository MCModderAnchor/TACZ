package com.tacz.guns.resource.pojo.data.gun;

import com.google.gson.annotations.SerializedName;

public class GunMeleeData {
    @SerializedName("distance")
    public float distance = 1f;

    @SerializedName("cooldown")
    public float cooldown = 1f;

    public float getDistance() {
        return distance;
    }

    public float getCooldown() {
        return cooldown;
    }
}
