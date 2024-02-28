package com.tac.guns.client.resource.pojo.display;

import com.google.gson.annotations.SerializedName;

import javax.annotation.Nullable;
import java.util.List;

public class GunDisplay {
    @SerializedName("model")
    private String modelLocation;
    @SerializedName("textures")
    private List<GunModelTexture> modelTextures;
    @Nullable
    @SerializedName("hud")
    private String hudTextureLocation;
    @Nullable
    @SerializedName("slot")
    private String slotTextureLocation;
    @Nullable
    @SerializedName("animation")
    private String animationLocation;
    @Nullable
    @SerializedName("transform")
    private GunTransform transform;
    @Nullable
    @SerializedName("shell")
    private ShellEjection shellEjection;

    public String getModelLocation() {
        return modelLocation;
    }

    public List<GunModelTexture> getModelTextures() {
        return modelTextures;
    }

    @Nullable
    public String getHudTextureLocation() {
        return hudTextureLocation;
    }

    @Nullable
    public String getSlotTextureLocation() {
        return slotTextureLocation;
    }

    @Nullable
    public String getAnimationLocation() {
        return animationLocation;
    }

    @Nullable
    public GunTransform getTransform() {
        return transform;
    }

    @Nullable
    public ShellEjection getShellEjection() {
        return shellEjection;
    }
}
