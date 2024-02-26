package com.tac.guns.client.resource.model.bedrock.pojo;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class BedrockGunModelPOJO {
    @SerializedName("model")
    private ResourceLocation modelLocation;

    @SerializedName("textures")
    private List<GunModelTexture> textures;

    @SerializedName("shell_ejection")
    private GunModelShellEjection shellEjection;

    public ResourceLocation getModelLocation() {
        return modelLocation;
    }

    public List<GunModelTexture> getTextures() {
        return textures;
    }

    public GunModelShellEjection getShellEjection() {
        return shellEjection;
    }
}
