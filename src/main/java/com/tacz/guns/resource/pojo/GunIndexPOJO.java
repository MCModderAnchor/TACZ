package com.tacz.guns.resource.pojo;

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

    @SerializedName("item_type")
    private String itemType = "modern_kinetic";

    @SerializedName("sort")
    private int sort;

    // 仅供同步，由服务端计算
    @SerializedName("has_unload_ammo_script")
    private boolean hasUnloadAmmoScript;

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

    public String getItemType() {
        return itemType;
    }

    public int getSort() {
        return sort;
    }

    public boolean hasUnloadAmmoScript() {
        return hasUnloadAmmoScript;
    }
}
