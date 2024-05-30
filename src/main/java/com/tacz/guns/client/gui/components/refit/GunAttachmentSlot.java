package com.tacz.guns.client.gui.components.refit;

import com.mojang.blaze3d.systems.RenderSystem;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.client.gui.GunRefitScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.function.Consumer;

public class GunAttachmentSlot extends Button implements IStackTooltip {
    private final AttachmentType type;
    private final Inventory inventory;
    private final int gunItemIndex;
    private final String nameKey;
    private boolean selected = false;
    private ItemStack attachmentItem = ItemStack.EMPTY;

    public GunAttachmentSlot(int pX, int pY, AttachmentType type, int gunItemIndex, Inventory inventory, Button.OnPress onPress) {
        super(pX, pY, GunRefitScreen.SLOT_SIZE, GunRefitScreen.SLOT_SIZE, Component.empty(), onPress, Button.DEFAULT_NARRATION);
        this.type = type;
        this.inventory = inventory;
        this.gunItemIndex = gunItemIndex;
        this.nameKey = String.format("tooltip.tacz.attachment.%s", type.name().toLowerCase(Locale.US));
    }

    @Override
    public void renderTooltip(Consumer<ItemStack> consumer) {
        if (this.isHoveredOrFocused() && !attachmentItem.isEmpty()) {
            consumer.accept(attachmentItem);
        }
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
        if (this.isHoveredOrFocused()) {
            Font font = Minecraft.getInstance().font;
            int yOffset = this.getY() + 20;
            if (this.selected && !attachmentItem.isEmpty()) {
                yOffset = this.getY() + 30;
            }
            graphics.drawCenteredString(font, Component.translatable(nameKey), this.getX() + this.getWidth() / 2, yOffset, ChatFormatting.WHITE.getColor());
        }
        ItemStack gunItem = inventory.getItem(gunItemIndex);
        IGun iGun = IGun.getIGunOrNull(gunItem);
        if (iGun == null) {
            return;
        }

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        // 渲染外框
        int x = this.getX();
        int y = this.getY();
        if (isHoveredOrFocused() || selected) {
            graphics.blit(GunRefitScreen.SLOT_TEXTURE, x, y, 0, 0, width, height, GunRefitScreen.SLOT_SIZE, GunRefitScreen.SLOT_SIZE);
        } else {
            graphics.blit(GunRefitScreen.SLOT_TEXTURE, x + 1, y + 1, 1, 1, width - 2, height - 2, GunRefitScreen.SLOT_SIZE, GunRefitScreen.SLOT_SIZE);
        }
        // 渲染内部物品，或者空置时的icon
        this.attachmentItem = iGun.getAttachment(gunItem, type);
        if (!attachmentItem.isEmpty()) {
            graphics.renderItem(attachmentItem, x + 1, y + 1);
        } else {
            int xOffset = GunRefitScreen.getSlotTextureXOffset(gunItem, type);
            graphics.blit(GunRefitScreen.ICONS_TEXTURE, x + 2, y + 2, width - 4, height - 4, xOffset, 0, GunRefitScreen.ICON_UV_SIZE, GunRefitScreen.ICON_UV_SIZE, GunRefitScreen.getSlotsTextureWidth(), GunRefitScreen.ICON_UV_SIZE);
        }

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public AttachmentType getType() {
        return type;
    }

    public ItemStack getAttachmentItem() {
        ItemStack gunItem = inventory.getItem(gunItemIndex);
        IGun iGun = IGun.getIGunOrNull(gunItem);
        if (iGun == null) {
            return ItemStack.EMPTY;
        }
        return iGun.getAttachment(gunItem, type);
    }

    public boolean isAllow() {
        ItemStack gunItem = inventory.getItem(gunItemIndex);
        IGun iGun = IGun.getIGunOrNull(gunItem);
        if (iGun == null) {
            return false;
        }
        return iGun.allowAttachmentType(gunItem, type);
    }
}
