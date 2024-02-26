package com.tac.guns.client.resource.model.bedrock.pojo;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;

public class GunModelAttachmentTexture {
    @SerializedName("key")
    private ResourceLocation key;

    @SerializedName("location")
    private ResourceLocation location;

    public ResourceLocation getKey() {
        return key;
    }

    public ResourceLocation getLocation() {
        return location;
    }
}
