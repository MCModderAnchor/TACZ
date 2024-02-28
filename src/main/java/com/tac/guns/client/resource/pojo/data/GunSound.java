package com.tac.guns.client.resource.pojo.data;

import com.google.gson.annotations.SerializedName;

public class GunSound {
    @SerializedName("shoot")
    private String shootSoundLocation;

    @SerializedName("reload")
    private String reloadSoundLocation;

    @SerializedName("inspect")
    private String inspectSoundLocation;

    @SerializedName("draw")
    private String drawSoundLocation;

    public String getShootSoundLocation() {
        return shootSoundLocation;
    }

    public String getReloadSoundLocation() {
        return reloadSoundLocation;
    }

    public String getInspectSoundLocation() {
        return inspectSoundLocation;
    }

    public String getDrawSoundLocation() {
        return drawSoundLocation;
    }
}