package com.tac.guns.client.tooltip;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import com.tac.guns.inventory.tooltip.AmmoBoxTooltip;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.item.ItemStack;

public class ClientAmmoBoxTooltip implements ClientTooltipComponent {
    private final ItemStack ammo;
    private final Component count;
    private final Component ammoName;

    public ClientAmmoBoxTooltip(AmmoBoxTooltip tooltip) {
        this.ammo = tooltip.getAmmo();
        this.count = new TranslatableComponent("tooltip.tac.ammo_box.count", tooltip.getCount());
        this.ammoName = this.ammo.getHoverName();
    }

    @Override
    public int getHeight() {
        return 28;
    }

    @Override
    public int getWidth(Font font) {
        return Math.max(font.width(ammoName), font.width(count)) + 22;
    }

    @Override
    public void renderText(Font font, int pX, int pY, Matrix4f matrix4f, MultiBufferSource.BufferSource bufferSource) {
        font.drawInBatch(ammoName, pX + 20, pY + 4, 0xffaa00, false, matrix4f, bufferSource, false, 0, 0xF000F0);
        font.drawInBatch(count, pX + 20, pY + 15, 0x666666, false, matrix4f, bufferSource, false, 0, 0xF000F0);
    }

    @Override
    public void renderImage(Font font, int mouseX, int mouseY, PoseStack poseStack, ItemRenderer itemRenderer, int blitOffset) {
        itemRenderer.renderGuiItem(ammo, mouseX, mouseY + 5);
    }
}
