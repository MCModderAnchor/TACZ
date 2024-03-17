package com.tac.guns.resource.pojo.data.gun;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AttachmentPass {
    @Nullable
    @SerializedName("white_list")
    private List<ResourceLocation> whiteList;

    @Nullable
    @SerializedName("black_list")
    private List<ResourceLocation> blackList;

    public boolean isAllow(ResourceLocation attachmentId) {
        if (whiteList != null && !whiteList.contains(attachmentId)) {
            return false;
        }
        return blackList == null || !blackList.contains(attachmentId);
    }
}
