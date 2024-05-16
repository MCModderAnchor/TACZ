package com.tacz.guns.client.gui.components.refit;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.client.gui.GunRefitScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class GunAttachmentSlot extends Button implements IComponentTooltip {
    private final AttachmentType type;
    private final Inventory inventory;
    private final int gunItemIndex;
    private final String nameKey;
    private boolean selected = false;
    private ItemStack attachmentItem = ItemStack.EMPTY;

    public GunAttachmentSlot(int pX, int pY, AttachmentType type, int gunItemIndex, Inventory inventory, Button.OnPress onPress) {
        super(pX, pY, GunRefitScreen.SLOT_SIZE, GunRefitScreen.SLOT_SIZE, Component.empty(), onPress);
        this.type = type;
        this.inventory = inventory;
        this.gunItemIndex = gunItemIndex;
        this.nameKey = String.format("tooltip.tacz.attachment.%s", type.name().toLowerCase(Locale.US));
    }

    @Override
    public void renderTooltip(Consumer<List<Component>> consumer) {
        if (this.isHovered) {
            List<Component> tips = Lists.newArrayList(Component.translatable(nameKey));
            if (!attachmentItem.isEmpty()) {
                tips.addAll(IComponentTooltip.getTooltipFromItem(attachmentItem));
            }
            consumer.accept(tips);
        }
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
            blit(poseStack, x, y, 0, 0, width, height, GunRefitScreen.SLOT_SIZE, GunRefitScreen.SLOT_SIZE);
        } else {
            blit(poseStack, x + 1, y + 1, 1, 1, width - 2, height - 2, GunRefitScreen.SLOT_SIZE, GunRefitScreen.SLOT_SIZE);
        }
        // 渲染内部物品，或者空置时的icon
        this.attachmentItem = iGun.getAttachment(gunItem, type);
        if (!attachmentItem.isEmpty()) {
            Minecraft.getInstance().getItemRenderer().renderGuiItem(attachmentItem, x + 1, y + 1);
        } else {
            RenderSystem.setShaderTexture(0, GunRefitScreen.ICONS_TEXTURE);
            int xOffset = GunRefitScreen.getSlotTextureXOffset(gunItem, type);
            blit(poseStack, x + 2, y + 2, width - 4, height - 4, xOffset, 0, GunRefitScreen.ICON_UV_SIZE, GunRefitScreen.ICON_UV_SIZE, GunRefitScreen.getSlotsTextureWidth(), GunRefitScreen.ICON_UV_SIZE);
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
