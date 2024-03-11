package com.tac.guns.client.resource.pojo.display.attachment;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;

public class AttachmentDisplay {
    @SerializedName("slot")
    private ResourceLocation slotTextureLocation;

    @SerializedName("model")
    private ResourceLocation model;

    @SerializedName("texture")
    private ResourceLocation texture;

    public ResourceLocation getSlotTextureLocation() {
        return slotTextureLocation;
    }

    public ResourceLocation getModel() {
        return model;
    }

    public ResourceLocation getTexture() {
        return texture;
    }
}
