package com.tacz.guns.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.tacz.guns.block.entity.AmmoStorageBlockEntity;
import com.tacz.guns.inventory.AmmoStorageMenu;
import com.tacz.guns.util.AmmoStorageHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class AmmoStorageScreen extends AbstractContainerScreen<AmmoStorageMenu> {
    private static final ResourceLocation CONTAINER_LOCATION = new ResourceLocation("textures/gui/container/generic_54.png");
    private static final int CONTAINER_ROWS = AmmoStorageMenu.STORAGE_ROWS;

    public AmmoStorageScreen(AmmoStorageMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 114 + CONTAINER_ROWS * 18;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int storageHeight = CONTAINER_ROWS * 18 + 17;
        graphics.blit(CONTAINER_LOCATION, leftPos, topPos, 0, 0, imageWidth, storageHeight);
        graphics.blit(CONTAINER_LOCATION, leftPos, topPos + storageHeight, 0, 126, imageWidth, 96);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderStorageCounts(graphics);
        renderTooltip(graphics, mouseX, mouseY);
    }

    private void renderStorageCounts(GuiGraphics graphics) {
        AmmoStorageBlockEntity storage = menu.getStorage();
        if (storage == null) {
            return;
        }
        for (int i = 0; i < AmmoStorageMenu.STORAGE_SLOT_COUNT; i++) {
            int count = storage.getStoredCount(i);
            ItemStack sample = storage.getSampleStack(i);
            if (sample.isEmpty() || count <= 1) {
                continue;
            }
            Slot slot = menu.slots.get(i);
            renderSizeLabel(graphics, leftPos + slot.x, topPos + slot.y, formatCount(count));
        }
    }

    private String formatCount(int count) {
        if (count >= 1_000_000) {
            return formatCompact(count, 1_000_000, "m");
        }
        if (count >= 1_000) {
            return formatCompact(count, 1_000, "k");
        }
        return Integer.toString(count);
    }

    private String formatCompact(int count, int unit, String suffix) {
        int whole = count / unit;
        int decimal = count % unit / (unit / 10);
        if (decimal == 0 || whole >= 10) {
            return whole + suffix;
        }
        return whole + "." + decimal + suffix;
    }

    private void renderSizeLabel(GuiGraphics graphics, int slotX, int slotY, String text) {
        float scale = 0.666F;
        float inverseScale = 1.0F / scale;
        int x = (int) ((slotX - 1 + 18.0F - font.width(text) * scale) * inverseScale);
        int y = (int) ((slotY - 1 + 16.0F - 5.0F * scale) * inverseScale);
        graphics.pose().pushPose();
        RenderSystem.disableDepthTest();
        graphics.pose().translate(0.0F, 0.0F, 300.0F);
        graphics.pose().scale(scale, scale, scale);
        graphics.drawString(font, text, x, y, 0xFFFFFFFF, true);
        RenderSystem.enableDepthTest();
        graphics.pose().popPose();
    }
}
