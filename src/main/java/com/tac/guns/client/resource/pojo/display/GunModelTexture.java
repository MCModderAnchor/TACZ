package com.tac.guns.client.resource.pojo.display;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

public class GunModelTexture {
    @SerializedName("id")
    private String id;

    @SerializedName("name")
    @Nullable
    private String name;

    @SerializedName("location")
    private ResourceLocation location;

    public String getId() {
        return id;
    }

    @Nullable
    public String getName() {
        return name;
    }

    public ResourceLocation getLocation() {
        return location;
    }
}
