package com.tacz.guns.client.resource.pojo.display.attachment;

import com.google.common.collect.Maps;
import com.google.gson.annotations.SerializedName;
import com.tacz.guns.client.resource.pojo.display.IDisplay;
import com.tacz.guns.client.resource.pojo.display.LaserConfig;
import com.tacz.guns.client.resource.pojo.display.gun.TextShow;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Map;

public class AttachmentDisplay implements IDisplay {
    @SerializedName("laser")
    private LaserConfig laserConfig;

    @SerializedName("slot")
    private ResourceLocation slotTextureLocation;

    @SerializedName("model")
    private ResourceLocation model;

    @SerializedName("texture")
    private ResourceLocation texture;

    @SerializedName("lod")
    @Nullable
    private AttachmentLod attachmentLod;

    @SerializedName("adapter")
    @Nullable
    private String adapterNodeName;

    @SerializedName("show_muzzle")
    private boolean showMuzzle = false;

    @SerializedName("show_mount")
    private boolean showMount = true;

    @SerializedName("text_show")
    private Map<String, TextShow> textShows = Maps.newHashMap();

    @SerializedName("zoom")
    @Nullable
    private float[] zoom;

    @SerializedName("views")
    @Nullable
    private int[] views;

    @SerializedName("scope")
    private boolean isScope = false;

    @SerializedName("sight")
    private boolean isSight = false;

    @SerializedName("fov")
    private float fov = 70;

    @SerializedName("views_fov")
    @Nullable
    private float[] viewsFov;

    @SerializedName("idle_scope_height_adjust")
    private float idleScopeHeightAdjust = 0;

    @SerializedName("sounds")
    private Map<String, ResourceLocation> sounds = Maps.newHashMap();

    public ResourceLocation getSlotTextureLocation() {
        return slotTextureLocation;
    }

    public ResourceLocation getModel() {
        return model;
    }

    public ResourceLocation getTexture() {
        return texture;
    }

    @Nullable
    public AttachmentLod getAttachmentLod() {
        return attachmentLod;
    }

    @Nullable
    public String getAdapterNodeName() {
        return adapterNodeName;
    }

    public boolean isShowMuzzle() {
        return showMuzzle;
    }

    public boolean isShowMount() {
        return showMount;
    }

    public Map<String, TextShow> getTextShows() {
        return textShows;
    }

    @Nullable
    public float[] getZoom() {
        return zoom;
    }

    @Nullable
    public int[] getViews() {
        return views;
    }

    public boolean isScope() {
        return isScope;
    }

    public boolean isSight() {
        return isSight;
    }

    public float getFov() {
        return fov;
    }

    @Nullable
    public float[] getViewsFov() {
        return viewsFov;
    }

    public float getIdleScopeHeightAdjust() {
        return idleScopeHeightAdjust;
    }

    public Map<String, ResourceLocation> getSounds() {
        return sounds;
    }

    @Nullable
    public LaserConfig getLaserConfig() {
        return laserConfig;
    }

    @Override
    public void init() {
        if (slotTextureLocation != null) {
            slotTextureLocation = converter.idToFile(slotTextureLocation);
        }
        if (texture != null) {
            texture = converter.idToFile(texture);
        }
        if (attachmentLod != null && attachmentLod.modelTexture != null) {
            attachmentLod.modelTexture = converter.idToFile(attachmentLod.modelTexture);
        }
    }
}
