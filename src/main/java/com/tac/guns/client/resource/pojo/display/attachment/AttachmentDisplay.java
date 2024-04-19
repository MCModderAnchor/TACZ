package com.tac.guns.client.resource.pojo.display.attachment;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

public class AttachmentDisplay {
    @SerializedName("slot")
    private ResourceLocation slotTextureLocation;

    @SerializedName("model")
    private ResourceLocation model;

    @SerializedName("texture")
    private ResourceLocation texture;

    @SerializedName("adapter")
    @Nullable
    private String adapterNodeName;

    @SerializedName("zoom")
    @Nullable
    private float[] zoom;

    @SerializedName("scope")
    private boolean isScope = false;

    @SerializedName("sight")
    private boolean isSight = false;

    @SerializedName("fov")
    private float fov = 70;

    public ResourceLocation getSlotTextureLocation() {
        return slotTextureLocation;
    }

    public ResourceLocation getModel() {
        return model;
    }

    public ResourceLocation getTexture() {
        return texture;
    }

    @Nullable
    public String getAdapterNodeName() {
        return adapterNodeName;
    }

    @Nullable
    public float[] getZoom() {
        return zoom;
    }

    public boolean isScope() {
        return isScope;
    }

    public boolean isSight() {
        return isSight;
    }

    public float getFov() {
        return fov;
    }
}
