package com.tacz.guns.resource.pojo.data.gun;

import com.google.gson.annotations.SerializedName;

public class GunReloadData {
    @SerializedName("type")
    private FeedType type = FeedType.MAGAZINE;

    @SerializedName("feed")
    private GunReloadTime feed = new GunReloadTime();

    @SerializedName("cooldown")
    private GunReloadTime cooldown = new GunReloadTime();

    public FeedType getType() {
        return type;
    }

    public GunReloadTime getFeed() {
        return feed;
    }

    public GunReloadTime getCooldown() {
        return cooldown;
    }
}
