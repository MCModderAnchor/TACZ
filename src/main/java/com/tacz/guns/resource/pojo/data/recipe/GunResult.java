package com.tacz.guns.resource.pojo.data.recipe;

import com.google.common.collect.Maps;
import com.google.gson.annotations.SerializedName;
import com.tacz.guns.api.item.attachment.AttachmentType;
import net.minecraft.resources.ResourceLocation;

import java.util.EnumMap;
import java.util.Map;

public class GunResult {
    @SerializedName("ammo_count")
    private int ammoCount = 0;

    @SerializedName("attachments")
    private Map<AttachmentType, ResourceLocation> attachments = Maps.newHashMap();

    public int getAmmoCount() {
        return ammoCount;
    }

    public Map<AttachmentType, ResourceLocation> getAttachments() {
        return attachments;
    }
}
