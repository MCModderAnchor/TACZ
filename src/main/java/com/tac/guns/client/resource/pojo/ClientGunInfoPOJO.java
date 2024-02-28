package com.tac.guns.client.resource.pojo;

import com.google.gson.annotations.SerializedName;
import com.tac.guns.client.resource.pojo.display.GunDisplay;

public class ClientGunInfoPOJO {
    @SerializedName("name")
    private String name;

    @SerializedName("tooltip")
    private String tooltip;

    @SerializedName("display")
    private GunDisplay display;

    public String getName() {
        return name;
    }

    public String getTooltip() {
        return tooltip;
    }

    public GunDisplay getDisplay() {
        return display;
    }
}
