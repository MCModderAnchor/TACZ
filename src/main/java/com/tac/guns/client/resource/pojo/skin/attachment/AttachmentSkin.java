package com.tac.guns.client.resource.pojo.skin.attachment;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;

public class AttachmentSkin {
    @SerializedName("parent")
    private ResourceLocation parent;
    @SerializedName("class")
    private String skinClass;
    @SerializedName("name")
    private String name;
    @SerializedName("model")
    private ResourceLocation model;
    @SerializedName("texture")
    private ResourceLocation texture;
    @SerializedName("transform")
    private AttachmentTransform transform;

    public ResourceLocation getParent() {
        return parent;
    }

    public String getSkinClass() {
        return skinClass;
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

    public AttachmentTransform getTransform() {
        return transform;
    }
}
