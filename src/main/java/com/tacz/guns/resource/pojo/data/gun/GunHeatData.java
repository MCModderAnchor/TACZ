package com.tacz.guns.resource.pojo.data.gun;

import com.google.gson.annotations.SerializedName;

public class GunHeatData {

    @SerializedName("max")
    private float heatMax = 100f;

    @SerializedName("per_shot")
    private float heatPerShot = 3f;

    @SerializedName("cooling_multiplier")
    private float coolingMultiplier = 1f;

    @SerializedName("cooling_delay")
    private long coolingDelay = 1000L; //ms

    @SerializedName("over_heat_time")
    private long overHeatTime = 3000L; //ms

    /** Heat removed per inventory tick, without cooling delays. Zero disables it. */
    @SerializedName("inventory_cooling_per_tick")
    private float inventoryCoolingPerTick = 0f;

    @SerializedName("min_inaccuracy")
    private float minInaccuracy = 1f;

    @SerializedName("max_inaccuracy")
    private float maxInaccuracy = 1f;

    @SerializedName("min_rpm_mod")
    private float minRpmMod = 1f;

    @SerializedName("max_rpm_mod")
    private float maxRpmMod = 1f;

    public long getCoolingDelay() {
        return coolingDelay;
    }

    public float getInventoryCoolingPerTick() {
        return inventoryCoolingPerTick;
    }

    public float getHeatMax() {
        return heatMax;
    }

    public float getHeatPerShot() {
        return heatPerShot;
    }

    public long getOverHeatTime() {
        return overHeatTime;
    }

    public float getMinInaccuracy() {
        return minInaccuracy;
    }

    public float getMaxInaccuracy() {
        return maxInaccuracy;
    }

    public float getCoolingMultiplier() {
        return coolingMultiplier;
    }

    public float getMinRpmMod() {
        return minRpmMod;
    }

    public float getMaxRpmMod() {
        return maxRpmMod;
    }
}
