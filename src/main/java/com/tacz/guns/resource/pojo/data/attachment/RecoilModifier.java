package com.tacz.guns.resource.pojo.data.attachment;

import com.google.gson.annotations.SerializedName;

public class RecoilModifier {
    @SerializedName("pitch")
    private float pitch;

    @SerializedName("yaw")
    private float yaw;

    public float getPitch() {
        return pitch;
    }

    public float getYaw() {
        return yaw;
    }
}
