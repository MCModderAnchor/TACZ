package com.tac.guns.api.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public interface IAttachment {
    /**
     * 该物品是否为配件
     */
    static boolean isAttachment(@Nullable ItemStack stack) {
        if (stack == null) {
            return false;
        }
        return stack.getItem() instanceof IAttachment;
    }

    ResourceLocation getAttachmentId(ItemStack attachmentStack);
    void setAttachmentId(ItemStack attachmentStack, @Nullable ResourceLocation gunId);
}
