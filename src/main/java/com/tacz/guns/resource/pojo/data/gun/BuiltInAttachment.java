package com.tacz.guns.resource.pojo.data.gun;

import com.google.common.collect.Lists;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BuiltInAttachment {
    private final List<ResourceLocation> attachments = Lists.newArrayList();
    @Nullable
    private ResourceLocation defaultAttachment = null;
    @Nullable
    private ResourceLocation displayAttachment = null;
    private boolean showIcon = true;

    public BuiltInAttachment(List<ResourceLocation> attachments, @Nullable ResourceLocation defaultAttachment, @Nullable ResourceLocation displayAttachment, boolean showIcon) {
        this.attachments.addAll(attachments);
        this.defaultAttachment = defaultAttachment;
        this.displayAttachment = displayAttachment;
        this.showIcon = showIcon;
    }

    public List<ResourceLocation> getAttachments() {
        return attachments;
    }

    @Nullable
    public ResourceLocation getDefaultAttachment() {
        return defaultAttachment;
    }

    @Nullable
    public ResourceLocation getDisplayAttachment() {
        return displayAttachment;
    }

    public boolean isShowIcon() {
        return showIcon;
    }
}
