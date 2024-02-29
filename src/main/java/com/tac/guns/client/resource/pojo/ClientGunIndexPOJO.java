package com.tac.guns.client.resource.pojo;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;

public class ClientGunIndexPOJO {
    @SerializedName("name")
    private String name;

    @SerializedName("tooltip")
    private String tooltip;

    @SerializedName("display")
    private ResourceLocation display;

    @SerializedName("data")
    private ResourceLocation data;

    public String getName() {
        return name;
    }

    public String getTooltip() {
        return tooltip;
    }

    public ResourceLocation getDisplay() {
        return display;
    }

    public ResourceLocation getData() {
        return data;
    }
}
