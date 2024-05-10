package com.tacz.guns.api.item.nbt;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.item.IAttachment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public interface AttachmentItemDataAccessor extends IAttachment {
    String ATTACHMENT_ID_TAG = "AttachmentId";
    String SKIN_ID_TAG = "Skin";
    String ZOOM_NUMBER_TAG = "ZoomNumber";

    @Override
    @Nonnull
    default ResourceLocation getAttachmentId(ItemStack attachmentStack) {
        CompoundTag nbt = attachmentStack.getOrCreateTag();
        if (nbt.contains(ATTACHMENT_ID_TAG, Tag.TAG_STRING)) {
            ResourceLocation attachmentId = ResourceLocation.tryParse(nbt.getString(ATTACHMENT_ID_TAG));
            return Objects.requireNonNullElse(attachmentId, DefaultAssets.EMPTY_ATTACHMENT_ID);
        }
        return DefaultAssets.EMPTY_ATTACHMENT_ID;
    }

    @Override
    default void setAttachmentId(ItemStack attachmentStack, @Nullable ResourceLocation attachmentId) {
        CompoundTag nbt = attachmentStack.getOrCreateTag();
        if (attachmentId != null) {
            nbt.putString(ATTACHMENT_ID_TAG, attachmentId.toString());
        }
    }

    @Override
    @Nullable
    default ResourceLocation getSkinId(ItemStack attachmentStack) {
        CompoundTag nbt = attachmentStack.getOrCreateTag();
        if (nbt.contains(SKIN_ID_TAG, Tag.TAG_STRING)) {
            return ResourceLocation.tryParse(nbt.getString(SKIN_ID_TAG));
        }
        return null;
    }

    @Override
    default void setSkinId(ItemStack attachmentStack, @Nullable ResourceLocation skinId) {
        CompoundTag nbt = attachmentStack.getOrCreateTag();
        if (skinId != null) {
            nbt.putString(SKIN_ID_TAG, skinId.toString());
        } else {
            nbt.remove(SKIN_ID_TAG);
        }
    }

    @Override
    default int getZoomNumber(ItemStack attachmentStack) {
        CompoundTag nbt = attachmentStack.getOrCreateTag();
        if (nbt.contains(ZOOM_NUMBER_TAG, Tag.TAG_INT)) {
            return nbt.getInt(ZOOM_NUMBER_TAG);
        }
        return 0;
    }

    @Override
    default void setZoomNumber(ItemStack attachmentStack, int zoomNumber) {
        CompoundTag nbt = attachmentStack.getOrCreateTag();
        nbt.putInt(ZOOM_NUMBER_TAG, zoomNumber);
    }
}
