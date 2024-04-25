package com.tac.guns.client.resource.pojo.display.ammo;

import com.google.gson.annotations.SerializedName;
import com.tac.guns.client.resource.loader.ShellDisplay;
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

    @Nullable
    @SerializedName("entity")
    private AmmoEntityDisplay ammoEntity;

    @Nullable
    @SerializedName("shell")
    private ShellDisplay shellDisplay;

    @Nullable
    @SerializedName("particle")
    private AmmoParticle particle;

    @SerializedName("tracer_color")
    private String tracerColor = "0xFFFFFF";

    @Nullable
    @SerializedName("transform")
    private AmmoTransform transform;

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

    @Nullable
    public AmmoEntityDisplay getAmmoEntity() {
        return ammoEntity;
    }

    @Nullable
    public ShellDisplay getShellDisplay() {
        return shellDisplay;
    }

    @Nullable
    public AmmoParticle getParticle() {
        return particle;
    }

    public String getTracerColor() {
        return tracerColor;
    }

    @Nullable
    public AmmoTransform getTransform() {
        return transform;
    }
}
