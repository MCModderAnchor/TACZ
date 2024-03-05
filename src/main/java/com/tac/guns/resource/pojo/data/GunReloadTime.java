package com.tac.guns.resource.pojo.data;

import com.google.gson.annotations.SerializedName;

public class GunReloadTime {
    @SerializedName("empty")
    private float emptyTime;

    @SerializedName("tactical")
    private float tacticalTime;

    public float getEmptyTime() {
        return emptyTime;
    }

    public float getTacticalTime() {
        return tacticalTime;
    }
}
