package com.tac.guns.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tac.guns.client.gui.components.refit.RefitSlotButton;
import com.tac.guns.inventory.GunRefitMenu;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

public class GunRefitScreen extends AbstractContainerScreen<GunRefitMenu> {
    public GunRefitScreen(GunRefitMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        this.imageWidth = width;
        this.imageHeight = height;
        super.init();
        this.clearWidgets();
        addRenderableWidget(new RefitSlotButton(leftPos + 50, topPos + 50, ItemStack.EMPTY, b -> {
            sendButtonClick(0);
        }));
    }

    @Override
    protected void renderLabels(@Nonnull PoseStack poseStack, int mouseX, int mouseY) {
    }

    @Override
    protected void renderBg(@Nonnull PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
    }

    private void sendButtonClick(int buttonId) {
        MultiPlayerGameMode gameMode = this.getMinecraft().gameMode;
        if (gameMode != null) {
            gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
        }
    }
}
