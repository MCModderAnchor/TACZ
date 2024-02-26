package com.tac.guns.client.resource.pojo.model;

import com.google.gson.annotations.SerializedName;
import com.tac.guns.client.resource.pojo.other.GunModelShellEjection;
import com.tac.guns.client.resource.pojo.texture.GunModelTexture;

import java.util.List;

public class BedrockGunPOJO {
    @SerializedName("model")
    private String modelLocation;

    @SerializedName("textures")
    private List<GunModelTexture> textures;

    @SerializedName("shell_ejection")
    private GunModelShellEjection shellEjection;

    public String getModelLocation() {
        return modelLocation;
    }

    public List<GunModelTexture> getTextures() {
        return textures;
    }

    public GunModelShellEjection getShellEjection() {
        return shellEjection;
    }
}
