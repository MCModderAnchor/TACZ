package com.tacz.guns.inventory.tooltip;

import com.tacz.guns.api.item.attachment.AttachmentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

public class AttachmentItemTooltip implements TooltipComponent {
    private final ItemStack itemStack;
    private final ResourceLocation attachmentId;
    private final AttachmentType type;

    public AttachmentItemTooltip(ItemStack itemStack, ResourceLocation attachmentId, AttachmentType type) {
        this.itemStack = itemStack;
        this.attachmentId = attachmentId;
        this.type = type;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public ResourceLocation getAttachmentId() {
        return attachmentId;
    }

    public AttachmentType getType() {
        return type;
    }
}
