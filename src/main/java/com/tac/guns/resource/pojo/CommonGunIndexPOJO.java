package com.tac.guns.resource.pojo;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;

public class CommonGunIndexPOJO {
    @SerializedName("data")
    private ResourceLocation data;

    public ResourceLocation getData() {
        return data;
    }
}
