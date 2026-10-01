package com.tacz.guns.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.tacz.guns.inventory.AmmoStorageEntry;
import com.tacz.guns.inventory.AmmoStorageMenu;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ClientMessageAmmoStorageClick;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public class AmmoStorageScreen extends AbstractContainerScreen<AmmoStorageMenu> {
    private static final ResourceLocation CONTAINER_LOCATION = ResourceLocation.parse("textures/gui/container/generic_54.png");
    private static final int CONTAINER_ROWS = AmmoStorageMenu.STORAGE_ROWS;
    private int virtualClickButton = -1;

    public AmmoStorageScreen(AmmoStorageMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 114 + CONTAINER_ROWS * 18 + AmmoStorageMenu.STATUS_HEIGHT;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        int storageHeight = CONTAINER_ROWS * 18 + 17 + AmmoStorageMenu.STATUS_HEIGHT;
        graphics.blit(CONTAINER_LOCATION, leftPos, topPos, 0, 0, imageWidth, 17);
        graphics.blit(CONTAINER_LOCATION, leftPos, topPos + 17, imageWidth, AmmoStorageMenu.STATUS_HEIGHT,
                0.0F, 7.0F, imageWidth, 1, 256, 256);
        graphics.blit(CONTAINER_LOCATION, leftPos, topPos + 17 + AmmoStorageMenu.STATUS_HEIGHT,
                0, 17, imageWidth, CONTAINER_ROWS * 18);
        graphics.blit(CONTAINER_LOCATION, leftPos, topPos + storageHeight, 0, 126, imageWidth, 96);
        renderCapacity(graphics);
        var entries = menu.getEntries();
        for (int i = 0; i < entries.size(); i++) {
            AmmoStorageEntry entry = entries.get(i);
            int x = leftPos + AmmoStorageMenu.INVENTORY_X + i % AmmoStorageMenu.STORAGE_COLUMNS * 18;
            int y = topPos + AmmoStorageMenu.STORAGE_Y + i / AmmoStorageMenu.STORAGE_COLUMNS * 18;
            graphics.renderItem(entry.sample(), x, y);
            renderSizeLabel(graphics, x, y, formatCount(entry.count()));
        }
        int hovered = getVirtualCell(mouseX, mouseY);
        if (hovered >= 0) {
            int x = leftPos + AmmoStorageMenu.INVENTORY_X + hovered % AmmoStorageMenu.STORAGE_COLUMNS * 18;
            int y = topPos + AmmoStorageMenu.STORAGE_Y + hovered / AmmoStorageMenu.STORAGE_COLUMNS * 18;
            renderSlotHighlight(graphics, x, y, 0);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (isHovering(8, 17, 160, 21, mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, List.of(
                    Component.translatable("gui.tacz.ammo_storage.capacity_detail", menu.getUsedStacks(), menu.getCapacityStacks()),
                    Component.translatable("gui.tacz.ammo_storage.free_capacity", Math.max(0, menu.getCapacityStacks() - menu.getUsedStacks()))
            ), mouseX, mouseY);
            return;
        }
        int cell = getVirtualCell(mouseX, mouseY);
        if (cell >= 0 && cell < menu.getEntries().size() && menu.getCarried().isEmpty()) {
            AmmoStorageEntry entry = menu.getEntries().get(cell);
            var tooltip = new ArrayList<>(getTooltipFromContainerItem(entry.sample()));
            tooltip.add(Component.translatable("gui.tacz.ammo_storage.count", entry.count()).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("gui.tacz.ammo_storage.remaining", entry.remainingCapacity())
                    .withStyle(entry.remainingCapacity() == 0 ? ChatFormatting.RED : ChatFormatting.GRAY));
            graphics.renderTooltip(font, tooltip, entry.sample().getTooltipImage(), entry.sample(), mouseX, mouseY);
            return;
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int cell = getVirtualCell(mouseX, mouseY);
        if (cell >= 0 && (button == 0 || button == 1)) {
            int entryId = cell < menu.getEntries().size() ? menu.getEntries().get(cell).id() : -1;
            AmmoStorageMenu.Action action = hasShiftDown() ? AmmoStorageMenu.Action.QUICK_MOVE
                    : button == 1 ? AmmoStorageMenu.Action.PICKUP_HALF : AmmoStorageMenu.Action.PICKUP;
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(new ClientMessageAmmoStorageClick(menu.containerId, entryId, action));
            virtualClickButton = button;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return virtualClickButton >= 0 || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == virtualClickButton) {
            virtualClickButton = -1;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private int getVirtualCell(double mouseX, double mouseY) {
        for (int cell = 0; cell < AmmoStorageMenu.STORAGE_SLOT_COUNT; cell++) {
            int x = AmmoStorageMenu.INVENTORY_X + cell % AmmoStorageMenu.STORAGE_COLUMNS * 18;
            int y = AmmoStorageMenu.STORAGE_Y + cell / AmmoStorageMenu.STORAGE_COLUMNS * 18;
            if (isHovering(x, y, 16, 16, mouseX, mouseY)) {
                return cell;
            }
        }
        return -1;
    }

    private void renderCapacity(GuiGraphics graphics) {
        Component capacity = Component.translatable("gui.tacz.ammo_storage.capacity", menu.getUsedStacks(), menu.getCapacityStacks());
        Component types = Component.translatable("gui.tacz.ammo_storage.types", menu.getEntries().size(), AmmoStorageMenu.STORAGE_SLOT_COUNT);
        graphics.drawString(font, capacity, leftPos + 8, topPos + 18, 0x404040, false);
        graphics.drawString(font, types, leftPos + imageWidth - 8 - font.width(types), topPos + 18,
                menu.getEntries().size() == AmmoStorageMenu.STORAGE_SLOT_COUNT ? 0xAA3030 : 0x404040, false);
        int x = leftPos + 8;
        int y = topPos + 31;
        double fullness = menu.getCapacityStacks() <= 0 ? 0
                : Math.min(1, (double) menu.getUsedStacks() / menu.getCapacityStacks());
        int color = fullness >= 1 ? 0xFFC45A48 : fullness >= 0.75 ? 0xFFD7A744 : 0xFF65A85C;
        graphics.fill(x, y, x + 160, y + 6, 0xFF373737);
        graphics.fill(x + 1, y + 1, x + 159, y + 5, 0xFF777777);
        graphics.fill(x + 1, y + 1, x + 1 + (int) Math.round(158 * fullness), y + 5, color);
    }

    private String formatCount(long count) {
        if (count >= 1_000_000_000) {
            return formatCompact(count, 1_000_000_000, "b");
        }
        if (count >= 1_000_000) {
            return formatCompact(count, 1_000_000, "m");
        }
        if (count >= 1_000) {
            return formatCompact(count, 1_000, "k");
        }
        return Long.toString(count);
    }

    private String formatCompact(long count, int unit, String suffix) {
        long whole = count / unit;
        long decimal = count % unit / (unit / 10);
        return decimal == 0 || whole >= 10 ? whole + suffix : whole + "." + decimal + suffix;
    }

    private void renderSizeLabel(GuiGraphics graphics, int x, int y, String text) {
        float scale = 0.666F;
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 200);
        graphics.pose().scale(scale, scale, 1);
        graphics.drawString(font, text, (int) ((x + 17 - font.width(text) * scale) / scale),
                (int) ((y + 12) / scale), 0xFFFFFFFF, true);
        graphics.pose().popPose();
    }
}
