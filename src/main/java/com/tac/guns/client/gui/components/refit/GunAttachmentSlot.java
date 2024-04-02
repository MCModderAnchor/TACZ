package com.tac.guns.client.gui.components.refit;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tac.guns.api.attachment.AttachmentType;
import com.tac.guns.api.item.IGun;
import com.tac.guns.client.gui.GunRefitScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

public class GunAttachmentSlot extends Button {
    private final AttachmentType type;
    private final Inventory inventory;
    private final int gunItemIndex;
    private boolean selected = false;

    public GunAttachmentSlot(int pX, int pY, AttachmentType type, int gunItemIndex, Inventory inventory, Button.OnPress onPress) {
        super(pX, pY, 18, 18, TextComponent.EMPTY, onPress);
        this.type = type;
        this.inventory = inventory;
        this.gunItemIndex = gunItemIndex;
    }

    @Override
    public void renderButton(@Nonnull PoseStack poseStack, int pMouseX, int pMouseY, float pPartialTick) {
        ItemStack gunItem = inventory.getItem(gunItemIndex);
        IGun iGun = IGun.getIGunOrNull(gunItem);
        if (iGun == null) {
            return;
        }
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, GunRefitScreen.SLOT_TEXTURE);
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        // 渲染外框
        if (isHoveredOrFocused() || selected) {
            blit(poseStack, x, y, 0, 0, width, height, 18, 18);
        } else {
            blit(poseStack, x + 1, y + 1, 1, 1, width - 2, height - 2, 18, 18);
        }
        // 渲染内部物品，或者空置时的icon
        ItemStack attachmentItem = iGun.getAttachment(gunItem, type);
        if (!attachmentItem.isEmpty()) {
            Minecraft.getInstance().getItemRenderer().renderGuiItem(attachmentItem, x + 1, y + 1);
        } else {
            RenderSystem.setShaderTexture(0, GunRefitScreen.ICONS_TEXTURE);
            int xOffset = GunRefitScreen.getSlotTextureXOffset(gunItem, type);
            blit(poseStack, x + 2, y + 2, width - 4, height - 4, xOffset, 0, GunRefitScreen.ICON_SIZE, GunRefitScreen.ICON_SIZE, GunRefitScreen.getSlotsTextureWidth(), GunRefitScreen.ICON_SIZE);
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
