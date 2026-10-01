package com.tacz.guns.util;

import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.TimelessAPI;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class VirtualOemAttachment {
    private static final String VIRTUAL_OEM_TAG = "VirtualOemAttachment";

    private VirtualOemAttachment() {
    }

    public static boolean isAllowedVirtualOem(ResourceLocation gunId, ResourceLocation attachmentId, AttachmentType type) {
        return TimelessAPI.getCommonGunIndex(gunId)
                .map(index -> {
                    var builtInAttachment = index.getGunData().getBuiltInAttachments().get(type);
                    return builtInAttachment != null && builtInAttachment.getAttachments().contains(attachmentId);
                })
                .orElse(false);
    }

    public static boolean shouldShowIcon(ResourceLocation gunId, ResourceLocation attachmentId, AttachmentType type) {
        return TimelessAPI.getCommonGunIndex(gunId)
                .map(index -> {
                    var builtInAttachment = index.getGunData().getBuiltInAttachments().get(type);
                    return builtInAttachment != null && builtInAttachment.getAttachments().contains(attachmentId) && builtInAttachment.isShowIcon();
                })
                .orElse(false);
    }

    public static boolean isBuiltinAttachment(ResourceLocation attachmentId) {
        return TimelessAPI.getCommonAttachmentIndex(attachmentId)
                .map(index -> index.getPojo().isBuiltinAttachment())
                .orElse(false);
    }

    public static void mark(ItemStack attachmentStack) {
        attachmentStack.getOrCreateTag().putBoolean(VIRTUAL_OEM_TAG, true);
    }

    public static boolean isMarked(ItemStack attachmentStack) {
        CompoundTag tag = attachmentStack.getTag();
        return tag != null && tag.contains(VIRTUAL_OEM_TAG, Tag.TAG_BYTE) && tag.getBoolean(VIRTUAL_OEM_TAG);
    }
}
