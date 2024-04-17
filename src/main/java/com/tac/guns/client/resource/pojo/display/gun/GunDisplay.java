package com.tac.guns.client.resource.pojo.display.gun;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Map;

public class GunDisplay {
    @SerializedName("model")
    private ResourceLocation modelLocation;
    @SerializedName("texture")
    private ResourceLocation modelTexture;
    @Nullable
    @SerializedName("lod")
    private GunLod gunLod;
    @Nullable
    @SerializedName("hud")
    private ResourceLocation hudTextureLocation;
    @Nullable
    @SerializedName("slot")
    private ResourceLocation slotTextureLocation;
    @Nullable
    @SerializedName("third_person_animation")
    private String thirdPersonAnimation;
    @Nullable
    @SerializedName("animation")
    private ResourceLocation animationLocation;
    @Nullable
    @SerializedName("use_default_animation")
    private DefaultAnimation defaultAnimation;
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
    @SerializedName("muzzle_flash")
    private MuzzleFlash muzzleFlash;

    public ResourceLocation getModelLocation() {
        return modelLocation;
    }

    public ResourceLocation getModelTexture() {
        return modelTexture;
    }

    @Nullable
    public GunLod getGunLod() {
        return gunLod;
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
    public DefaultAnimation getDefaultAnimation() {
        return defaultAnimation;
    }

    @Nullable
    public String getThirdPersonAnimation() {
        return thirdPersonAnimation;
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
    public MuzzleFlash getMuzzleFlash() {
        return muzzleFlash;
    }
}
