package com.tac.guns.resource.pojo.data;

import com.google.gson.annotations.SerializedName;

public class GunReloadData {
    @SerializedName("type")
    private String type;

    @SerializedName("feed")
    private GunReloadTime feed;

    @SerializedName("cooldown")
    private GunReloadTime cooldown;

    public String getType() {
        return type;
    }

    public GunReloadTime getFeed() {
        return feed;
    }

    public GunReloadTime getCooldown() {
        return cooldown;
    }
}
