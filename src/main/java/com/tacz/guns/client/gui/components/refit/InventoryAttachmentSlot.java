package com.tacz.guns.client.gui.components.refit;

import com.mojang.blaze3d.systems.RenderSystem;
import com.tacz.guns.client.gui.GunRefitScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

public class InventoryAttachmentSlot extends Button implements IStackTooltip {
    private final int slotIndex;
    private final Inventory inventory;
    private final ItemStack displayStack;

    public InventoryAttachmentSlot(int pX, int pY, int slotIndex, Inventory inventory, Button.OnPress onPress) {
        this(pX, pY, slotIndex, inventory, ItemStack.EMPTY, onPress);
    }

    public InventoryAttachmentSlot(int pX, int pY, ItemStack displayStack, Inventory inventory, Button.OnPress onPress) {
        this(pX, pY, -1, inventory, displayStack, onPress);
    }

    private InventoryAttachmentSlot(int pX, int pY, int slotIndex, Inventory inventory, ItemStack displayStack, Button.OnPress onPress) {
        super(pX, pY, 18, 18, Component.empty(), onPress, DEFAULT_NARRATION);
        this.slotIndex = slotIndex;
        this.inventory = inventory;
        this.displayStack = displayStack;
    }

    @Override
    public void renderTooltip(Consumer<ItemStack> consumer) {
        if (this.isHoveredOrFocused()) {
            ItemStack item = getItemStack();
            if (!item.isEmpty()) {
                consumer.accept(item);
            }
        }
    }

    @Override
    public void renderWidget(@Nonnull GuiGraphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();

        int x = getX(), y = getY();
        if (isHoveredOrFocused()) {
            graphics.blit(GunRefitScreen.SLOT_TEXTURE, x, y, 0, 0, width, height, 18, 18);
        } else {
            graphics.blit(GunRefitScreen.SLOT_TEXTURE, x + 1, y + 1, 1, 1, width - 2, height - 2, 18, 18);
        }
        graphics.renderItem(getItemStack(), x + 1, y + 1);

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    public ItemStack getItemStack() {
        if (!displayStack.isEmpty()) {
            return displayStack;
        }
        if (0 <= this.slotIndex && this.slotIndex < this.inventory.getContainerSize()) {
            return this.inventory.getItem(slotIndex);
        }
        return ItemStack.EMPTY;
    }
}
