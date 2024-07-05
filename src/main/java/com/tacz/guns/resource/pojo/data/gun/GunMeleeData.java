package com.tacz.guns.resource.pojo.data.gun;

import com.google.gson.annotations.SerializedName;

import javax.annotation.Nullable;

public class GunMeleeData {
    @SerializedName("distance")
    private float distance = 1f;

    @SerializedName("cooldown")
    private float cooldown = 1f;

    @SerializedName("default")
    @Nullable
    private GunDefaultMeleeData defaultMeleeData = null;

    public float getDistance() {
        return distance;
    }

    public float getCooldown() {
        return cooldown;
    }

    @Nullable
    public GunDefaultMeleeData getDefaultMeleeData() {
        return defaultMeleeData;
    }
}
