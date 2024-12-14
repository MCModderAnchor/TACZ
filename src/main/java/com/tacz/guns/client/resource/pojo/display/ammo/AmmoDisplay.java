package com.tacz.guns.client.resource.pojo.display.ammo;

import com.google.gson.annotations.SerializedName;
import com.tacz.guns.client.resource.pojo.display.IDisplay;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

public class AmmoDisplay implements IDisplay {
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

    @Override
    public void init() {
        if (modelTexture != null) {
            modelTexture = converter.idToFile(modelTexture);
        }
        if (slotTextureLocation != null) {
            slotTextureLocation = converter.idToFile(slotTextureLocation);
        }
        if (ammoEntity != null && ammoEntity.modelTexture != null) {
            ammoEntity.modelTexture = converter.idToFile(ammoEntity.modelTexture);
        }
        if (shellDisplay != null&& shellDisplay.modelTexture != null) {
            shellDisplay.modelTexture = converter.idToFile(shellDisplay.modelTexture);
        }
    }
}
