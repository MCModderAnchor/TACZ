package com.tac.guns.resource.pojo.data;

import com.google.gson.annotations.SerializedName;

public class GunReloadData {
    @SerializedName("type")
    private String type;

    @SerializedName("empty_mag_fed_time")
    private float emptyMagFedTime;

    @SerializedName("empty_reload_time")
    private float emptyReloadTime;

    @SerializedName("normal_mag_fed_time")
    private float normalMagFedTime;

    @SerializedName("normal_reload_time")
    private float normalReloadTime;

    public String getType() {
        return type;
    }

    public float getEmptyMagFedTime() {
        return emptyMagFedTime;
    }

    public float getEmptyReloadTime() {
        return emptyReloadTime;
    }

    public float getNormalMagFedTime() {
        return normalMagFedTime;
    }

    public float getNormalReloadTime() {
        return normalReloadTime;
    }
}
