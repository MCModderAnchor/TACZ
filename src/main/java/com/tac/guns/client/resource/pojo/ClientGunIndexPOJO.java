package com.tac.guns.client.resource.pojo;

import com.google.gson.annotations.SerializedName;

public class ClientGunIndexPOJO {
    @SerializedName("name")
    private String name;

    @SerializedName("tooltip")
    private String tooltip;

    public String getName() {
        return name;
    }

    public String getTooltip() {
        return tooltip;
    }
}
