package com.tac.guns.client.resource.pojo;

import com.google.gson.annotations.SerializedName;
import com.tac.guns.client.resource.pojo.data.GunData;
import com.tac.guns.client.resource.pojo.display.GunDisplay;

public class ClientGunIndexPOJO {
    @SerializedName("name")
    private String name;

    @SerializedName("tooltip")
    private String tooltip;

    @SerializedName("display")
    private GunDisplay display;

    @SerializedName("data")
    private GunData data;

    public String getName() {
        return name;
    }

    public String getTooltip() {
        return tooltip;
    }

    public GunDisplay getDisplay() {
        return display;
    }

    public GunData getData() {
        return data;
    }
}
