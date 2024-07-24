package com.tacz.guns.client.resource.pojo.display.gun;

import com.google.gson.annotations.SerializedName;

public class ControllableData {
    @SerializedName("low_frequency")
    private float lowFrequency = 0.25f;

    @SerializedName("high_frequency")
    private float highFrequency = 0.5f;

    @SerializedName("time")
    private int timeInMs = 80;

    public float getLowFrequency() {
        return lowFrequency;
    }

    public float getHighFrequency() {
        return highFrequency;
    }

    public int getTimeInMs() {
        return timeInMs;
    }
}
