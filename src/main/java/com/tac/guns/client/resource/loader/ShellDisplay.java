package com.tac.guns.client.resource.loader;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;

public class ShellDisplay {
    @SerializedName("model")
    private ResourceLocation modelLocation;
    @SerializedName("texture")
    private ResourceLocation modelTexture;

    public ResourceLocation getModelLocation() {
        return modelLocation;
    }

    public ResourceLocation getModelTexture() {
        return modelTexture;
    }
}
