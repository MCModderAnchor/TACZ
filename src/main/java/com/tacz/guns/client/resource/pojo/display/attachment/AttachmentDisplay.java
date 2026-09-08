package com.tacz.guns.client.resource.pojo.display.attachment;

import com.google.common.collect.Maps;
import com.google.gson.annotations.SerializedName;
import com.tacz.guns.client.resource.pojo.display.IDisplay;
import com.tacz.guns.client.resource.pojo.display.LaserConfig;
import com.tacz.guns.client.resource.pojo.display.SoundData;
import com.tacz.guns.client.resource.pojo.display.gun.TextShow;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

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

    /**
     * Blockbench 模型单位，指向枪口前方；16 单位等于一格。
     * 用于将第一人称枪口粒子发射原点沿枪口方向前移（如加装长消音器/制退器时）。
     */
    @SerializedName("muzzle_flash_offset")
    private float muzzleFlashOffset = 0.0f;

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
    private Map<String, SoundData> sounds = Maps.newHashMap();

    @SerializedName("mode")
    private String variantMode = "replace";

    @SerializedName("mount_offset")
    @Nullable
    private Vector3f mountOffset;

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

    public float getMuzzleFlashOffset() {
        return muzzleFlashOffset;
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

    public Map<String, SoundData> getSounds() {
        return sounds;
    }

    public String getVariantMode() {
        return variantMode == null ? "replace" : variantMode;
    }

    @Nullable
    public Vector3f getMountOffset() {
        return mountOffset;
    }

    @Nullable
    public LaserConfig getLaserConfig() {
        return laserConfig;
    }

    @Override
    public void init() {
        if (!Float.isFinite(muzzleFlashOffset) || muzzleFlashOffset < 0.0f) {
            muzzleFlashOffset = 0.0f;
        }
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
