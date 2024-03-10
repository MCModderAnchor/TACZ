package com.tac.guns.client.resource.pojo.display.gun;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public class GunDisplay {
    @SerializedName("model")
    private ResourceLocation modelLocation;
    @SerializedName("textures")
    private List<GunModelTexture> modelTextures;
    @Nullable
    @SerializedName("hud")
    private ResourceLocation hudTextureLocation;
    @Nullable
    @SerializedName("slot")
    private ResourceLocation slotTextureLocation;
    @Nullable
    @SerializedName("animation")
    private ResourceLocation animationLocation;
    @Nullable
    @SerializedName("sounds")
    private Map<String, ResourceLocation> sounds;
    @Nullable
    @SerializedName("transform")
    private GunTransform transform;
    @Nullable
    @SerializedName("shell")
    private ShellEjection shellEjection;
    @Nullable
    @SerializedName("ica")
    private AnimationInfluenceCoefficient animationInfluenceCoefficient;

    public ResourceLocation getModelLocation() {
        return modelLocation;
    }

    public List<GunModelTexture> getModelTextures() {
        return modelTextures;
    }

    @Nullable
    public ResourceLocation getHudTextureLocation() {
        return hudTextureLocation;
    }

    @Nullable
    public ResourceLocation getSlotTextureLocation() {
        return slotTextureLocation;
    }

    @Nullable
    public ResourceLocation getAnimationLocation() {
        return animationLocation;
    }

    @Nullable
    public Map<String, ResourceLocation> getSounds() {
        return sounds;
    }

    @Nullable
    public GunTransform getTransform() {
        return transform;
    }

    @Nullable
    public ShellEjection getShellEjection() {
        return shellEjection;
    }

    @Nullable
    public AnimationInfluenceCoefficient getAnimationInfluenceCoefficient() {
        return animationInfluenceCoefficient;
    }
}
