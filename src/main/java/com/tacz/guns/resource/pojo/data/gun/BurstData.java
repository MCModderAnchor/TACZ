package com.tacz.guns.resource.pojo.data.gun;

import com.google.gson.annotations.SerializedName;

public class BurstData {
    @SerializedName("continuous_shoot")
    private boolean continuousShoot = false;

    @SerializedName("count")
    private int count = 3;

    @SerializedName("bpm")
    private int bpm = 200;

    @SerializedName("min_interval")
    private double minInterval = 1;

    public int getCount() {
        return count;
    }

    public int getBpm() {
        return bpm;
    }

    public double getMinInterval() {
        return minInterval;
    }

    public boolean isContinuousShoot() {
        return continuousShoot;
    }
}
