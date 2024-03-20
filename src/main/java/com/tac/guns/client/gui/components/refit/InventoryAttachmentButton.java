package com.tac.guns.client.gui.components.refit;

import net.minecraft.world.item.ItemStack;

public class InventoryAttachmentButton extends RefitButton {
    private final int slotIndex;

    public InventoryAttachmentButton(int pX, int pY, int slotIndex, ItemStack stack, OnPress onPress) {
        super(pX, pY, stack, onPress);
        this.slotIndex = slotIndex;
    }

    public int getSlotIndex() {
        return slotIndex;
    }
}
