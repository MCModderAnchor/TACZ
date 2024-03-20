package com.tac.guns.client.gui.components.refit;

import com.tac.guns.api.attachment.AttachmentType;
import com.tac.guns.item.builder.AttachmentItemBuilder;
import net.minecraft.client.gui.components.Button;
import net.minecraft.world.item.ItemStack;

public class AttachmentTypeButton extends RefitButton {
    private final AttachmentType type;

    public AttachmentTypeButton(int pX, int pY, AttachmentType type, ItemStack stack, Button.OnPress onPress) {
        super(pX, pY, stack, onPress);
        this.type = type;
    }

    public AttachmentType getType() {
        return type;
    }
}
