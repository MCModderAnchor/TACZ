package com.tac.guns.client.resource.pojo.texture;

import com.google.gson.annotations.SerializedName;

import javax.annotation.Nullable;
import java.util.List;

public class GunModelTexture {
    @SerializedName("name")
    private String name;

    @SerializedName("location")
    private String location;

    @SerializedName("attachment")
    @Nullable
    private List<GunModelAttachmentTexture> attachmentTextures;

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    @Nullable
    public List<GunModelAttachmentTexture> getAttachmentTextures() {
        return attachmentTextures;
    }
}
