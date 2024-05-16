package com.tacz.guns.client.gui.components.refit;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.client.gui.GunRefitScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Consumer;

public class InventoryAttachmentSlot extends Button implements IComponentTooltip {
    private final int slotIndex;
    private final Inventory inventory;

    public InventoryAttachmentSlot(int pX, int pY, int slotIndex, Inventory inventory, Button.OnPress onPress) {
        super(pX, pY, 18, 18, Component.EMPTY, onPress);
        this.slotIndex = slotIndex;
        this.inventory = inventory;
    }

    @Override
    public void renderTooltip(Consumer<List<Component>> consumer) {
        if (this.isHovered && 0 <= this.slotIndex && this.slotIndex < this.inventory.getContainerSize()) {
            ItemStack item = this.inventory.getItem(slotIndex);
            consumer.accept(IComponentTooltip.getTooltipFromItem(item));
        }
    }

    @Override
    public void renderButton(@Nonnull PoseStack poseStack, int pMouseX, int pMouseY, float pPartialTick) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, GunRefitScreen.SLOT_TEXTURE);
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        if (isHoveredOrFocused()) {
            blit(poseStack, x, y, 0, 0, width, height, 18, 18);
        } else {
            blit(poseStack, x + 1, y + 1, 1, 1, width - 2, height - 2, 18, 18);
        }
        Minecraft.getInstance().getItemRenderer().renderGuiItem(inventory.getItem(slotIndex), x + 1, y + 1);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    public int getSlotIndex() {
        return slotIndex;
    }
}
