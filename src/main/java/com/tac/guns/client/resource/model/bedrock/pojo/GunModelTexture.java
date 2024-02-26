package com.tac.guns.client.resource.model.bedrock.pojo;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.List;

public class GunModelTexture {
    @SerializedName("name")
    private String name;

    @SerializedName("location")
    private ResourceLocation location;

    @SerializedName("attachment")
    @Nullable
    private List<GunModelAttachmentTexture> attachmentTextures;

    public String getName() {
        return name;
    }

    public ResourceLocation getLocation() {
        return location;
    }

    @Nullable
    public List<GunModelAttachmentTexture> getAttachmentTextures() {
        return attachmentTextures;
    }
}
