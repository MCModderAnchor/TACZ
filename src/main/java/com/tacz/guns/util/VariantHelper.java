package com.tacz.guns.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class VariantHelper {
    private static final String VARIANT_TAG = "Variant";

    private VariantHelper() {
    }

    public static void setVariant(ItemStack attachment, ResourceLocation variantId) {
        if (attachment.isEmpty() || variantId == null) {
            return;
        }
        attachment.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY, data -> data.update(tag -> tag.putString(VARIANT_TAG, variantId.toString())));
    }

    @Nullable
    public static ResourceLocation getVariant(ItemStack attachment) {
        if (attachment.isEmpty()) {
            return null;
        }
        CompoundTag tag = attachment.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        if (tag == null || !tag.contains(VARIANT_TAG, Tag.TAG_STRING)) {
            return null;
        }
        return ResourceLocation.tryParse(tag.getString(VARIANT_TAG));
    }

    public static boolean hasVariant(ItemStack attachment) {
        return getVariant(attachment) != null;
    }

    public static void removeVariant(ItemStack attachment) {
        if (attachment.isEmpty()) {
            return;
        }
        attachment.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY,
                data -> data.update(tag -> tag.remove(VARIANT_TAG)));
    }
}
