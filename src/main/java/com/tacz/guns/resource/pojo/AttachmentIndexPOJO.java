package com.tacz.guns.resource.pojo;

import com.google.gson.annotations.SerializedName;
import com.tacz.guns.api.item.attachment.AttachmentType;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

public class AttachmentIndexPOJO {
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
    private AttachmentType type;

    @SerializedName("sort")
    private int sort;

    @SerializedName("hidden")
    private boolean hidden = false;

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

    public AttachmentType getType() {
        return type;
    }

    public boolean isHidden() {
        return hidden;
    }

    public int getSort() {
        return sort;
    }
}
