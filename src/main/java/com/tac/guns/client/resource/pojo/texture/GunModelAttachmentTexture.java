package com.tac.guns.client.resource.pojo.texture;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;

public class GunModelAttachmentTexture {
    @SerializedName("key")
    private ResourceLocation key;

    @SerializedName("location")
    private String location;

    public ResourceLocation getKey() {
        return key;
    }

    public String getLocation() {
        return location;
    }
}