package com.tacz.guns.resource.pojo.data.gun;

import com.google.gson.annotations.SerializedName;

public class GunFireModeAdjust {
    @SerializedName("auto")
    private GunFireModeAdjustData auto = new GunFireModeAdjustData();

    @SerializedName("semi")
    private GunFireModeAdjustData semi = new GunFireModeAdjustData();

    @SerializedName("burst")
    private GunFireModeAdjustData burst = new GunFireModeAdjustData();

    public GunFireModeAdjustData getAuto() {
        return auto;
    }

    public GunFireModeAdjustData getSemi() {
        return semi;
    }

    public GunFireModeAdjustData getBurst() {
        return burst;
    }
}
