package com.tacz.guns.client.resource.pojo.skin.attachment;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;

public class AttachmentSkin {
    @SerializedName("parent")
    private ResourceLocation parent;
    @SerializedName("name")
    private String name;
    @SerializedName("model")
    private ResourceLocation model;
    @SerializedName("texture")
    private ResourceLocation texture;

    public ResourceLocation getParent() {
        return parent;
    }

    public String getName() {
        return name;
    }

    public ResourceLocation getModel() {
        return model;
    }

    public ResourceLocation getTexture() {
        return texture;
    }
}
