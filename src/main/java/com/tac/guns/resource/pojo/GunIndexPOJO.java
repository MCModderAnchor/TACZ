package com.tac.guns.resource.pojo;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

public class GunIndexPOJO {
    @SerializedName("name")
    private String name;

    @SerializedName("tooltip")
    @Nullable
    private String tooltip;

    @SerializedName("display")
    private ResourceLocation display;

    @SerializedName("data")
    private ResourceLocation data;

    @SerializedName("type")
    private String type;

    @SerializedName("sort")
    private int sort;

    public String getName() {
        return name;
    }

    @Nullable
    public String getTooltip() {
        return tooltip;
    }

    public ResourceLocation getDisplay() {
        return display;
    }

    public ResourceLocation getData() {
        return data;
    }

    public String getType() {
        return type;
    }

    public int getSort() {
        return sort;
    }
}
