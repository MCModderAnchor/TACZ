package com.tac.guns.resource.pojo.data.gun;

import com.google.gson.annotations.SerializedName;

public class MoveSpeed {
    @SerializedName("base")
    private float baseMultiplier = 1.0f;

    @SerializedName("aim")
    private float aimMultiplier = 0.5f;

    public float getBaseMultiplier() {
        return baseMultiplier;
    }

    public float getAimMultiplier() {
        return aimMultiplier;
    }
}
