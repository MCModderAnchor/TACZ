package com.tacz.guns.client.resource.pojo.display.gun;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;

public class MuzzleFlash {
    @SerializedName("texture")
    protected ResourceLocation texture = null;

    @SerializedName("scale")
    private float scale = 1;

    public ResourceLocation getTexture() {
        return texture;
    }

    public float getScale() {
        return scale;
    }
}
