package com.tac.guns.resource.pojo.data.gun;

import com.google.gson.annotations.SerializedName;
import com.tac.guns.api.attachment.AttachmentType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AttachmentPass {
    @SerializedName("type")
    private AttachmentType type;

    @Nullable
    @SerializedName("white_list")
    private List<ResourceLocation> whiteList;

    @Nullable
    @SerializedName("black_list")
    private List<ResourceLocation> blackList;

    public AttachmentType getType() {
        return type;
    }

    @Nullable
    public List<ResourceLocation> getWhiteList() {
        return whiteList;
    }

    @Nullable
    public List<ResourceLocation> getBlackList() {
        return blackList;
    }
}
