package com.tac.guns.api.item;

import com.tac.guns.api.attachment.AttachmentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface IAttachment {
    /**
     * @return 如果物品类型为 IAttachment 则返回显式转换后的实例，否则返回 null。
     */
    @Nullable
    static IAttachment getIAttachmentOrNull(@Nullable ItemStack stack) {
        if (stack == null) {
            return null;
        }
        if (stack.getItem() instanceof IAttachment iAttachment) {
            return iAttachment;
        }
        return null;
    }

    @Nonnull
    ResourceLocation getAttachmentId(ItemStack attachmentStack);

    void setAttachmentId(ItemStack attachmentStack, @Nullable ResourceLocation attachmentId);

    @Nullable
    ResourceLocation getSkinId(ItemStack attachmentStack);

    void setSkinId(ItemStack attachmentStack, @Nullable ResourceLocation skinId);

    @Nonnull
    AttachmentType getType(ItemStack attachmentStack);
}
