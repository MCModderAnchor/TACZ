package com.tac.guns.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tac.guns.inventory.GunRefitMenu;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.player.Inventory;

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
        addRenderableWidget(new Button(leftPos + 50, topPos + 50, 20, 20, TextComponent.EMPTY, b -> {
            sendButtonClick(0);
        }));
        addRenderableWidget(new Button(leftPos + 50, topPos + 80, 20, 20, TextComponent.EMPTY, b -> {
            sendButtonClick(1);
        }));
        addRenderableWidget(new Button(leftPos + 50, topPos + 110, 20, 20, TextComponent.EMPTY, b -> {
            sendButtonClick(2);
        }));
        addRenderableWidget(new Button(leftPos + 50, topPos + 140, 20, 20, TextComponent.EMPTY, b -> {
            sendButtonClick(3);
        }));
        addRenderableWidget(new Button(leftPos + 50, topPos + 170, 20, 20, TextComponent.EMPTY, b -> {
            sendButtonClick(4);
        }));
    }

    @Override
    protected void renderLabels(PoseStack poseStack, int mouseX, int mouseY) {
    }

    @Override
    protected void renderBg(PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
    }

    private void sendButtonClick(int buttonId) {
        MultiPlayerGameMode gameMode = this.getMinecraft().gameMode;
        if (gameMode != null) {
            gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
