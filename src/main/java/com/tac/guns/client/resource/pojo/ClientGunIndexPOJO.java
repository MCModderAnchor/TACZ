package com.tac.guns.client.resource.pojo;

import com.google.gson.annotations.SerializedName;

public class ClientGunIndexPOJO {
    @SerializedName("name")
    private String name;

    @SerializedName("tooltip")
    private String tooltip;

    @SerializedName("display")
    private String display;

    @SerializedName("data")
    private String data;

    public String getName() {
        return name;
    }

    public String getTooltip() {
        return tooltip;
    }

    public String getDisplay() {
        return display;
    }

    public String getData() {
        return data;
    }
}
