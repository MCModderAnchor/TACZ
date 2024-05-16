package com.tacz.guns.resource.pojo.data.gun;

import com.google.gson.annotations.SerializedName;

public class GunReloadTime {
    @SerializedName("empty")
    private float emptyTime = 2.5f;

    @SerializedName("tactical")
    private float tacticalTime = 2.0f;

    public float getEmptyTime() {
        return emptyTime;
    }

    public float getTacticalTime() {
        return tacticalTime;
    }
}
