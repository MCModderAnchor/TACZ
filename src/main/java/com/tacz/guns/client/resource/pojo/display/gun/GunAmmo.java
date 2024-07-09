package com.tacz.guns.client.resource.pojo.display.gun;

import com.google.gson.annotations.SerializedName;
import com.tacz.guns.client.resource.pojo.display.ammo.AmmoParticle;

import javax.annotation.Nullable;

public class GunAmmo {
    @Nullable
    @SerializedName("tracer_color")
    private String tracerColor = null;

    @Nullable
    @SerializedName("particle")
    private AmmoParticle particle = null;

    @Nullable
    public String getTracerColor() {
        return tracerColor;
    }

    @Nullable
    public AmmoParticle getParticle() {
        return particle;
    }
}
