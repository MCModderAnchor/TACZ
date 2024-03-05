package com.tac.guns.client.resource.pojo.display;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

public class AmmoDisplay {
    @SerializedName("model")
    private ResourceLocation modelLocation;
    @SerializedName("texture")
    private ResourceLocation modelTexture;
    @Nullable
    @SerializedName("slot")
    private ResourceLocation slotTextureLocation;

    public ResourceLocation getModelLocation() {
        return modelLocation;
    }

    public ResourceLocation getModelTexture() {
        return modelTexture;
    }

    @Nullable
    public ResourceLocation getSlotTextureLocation() {
        return slotTextureLocation;
    }
}
