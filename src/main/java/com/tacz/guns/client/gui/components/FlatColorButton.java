package com.tacz.guns.client.gui.components;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.Collections;
import java.util.List;

public class FlatColorButton extends Button {
    private boolean isSelect = false;
    private List<Component> tooltips;

    public FlatColorButton(int pX, int pY, int pWidth, int pHeight, Component pMessage, OnPress pOnPress) {
        super(pX, pY, pWidth, pHeight, pMessage, pOnPress);
    }

    public FlatColorButton setTooltips(String key) {
        tooltips = Collections.singletonList(Component.translatable(key));
        return this;
    }

    public FlatColorButton setTooltips(List<Component> tooltips) {
        this.tooltips = tooltips;
        return this;
    }

    public void renderToolTip(Screen screen, PoseStack pPoseStack, int pMouseX, int pMouseY) {
        if (this.isHovered && tooltips != null) {
            screen.renderComponentTooltip(pPoseStack, tooltips, pMouseX, pMouseY);
        }
    }

    @Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float pPartialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        if (isSelect) {
            fillGradient(poseStack, this.x, this.y, this.x + this.width, this.y + this.height, 0xAF222222, 0xAF222222);
        } else {
            fillGradient(poseStack, this.x, this.y, this.x + this.width, this.y + this.height, 0xAF222222, 0xAF222222);
        }
        if (this.isHoveredOrFocused()) {
            fillGradient(poseStack, this.x, this.y + 1, this.x + 1, this.y + this.height - 1, 0xff_F3EFE0, 0xff_F3EFE0);
            fillGradient(poseStack, this.x, this.y, this.x + this.width, this.y + 1, 0xff_F3EFE0, 0xff_F3EFE0);
            fillGradient(poseStack, this.x + this.width - 1, this.y + 1, this.x + this.width, this.y + this.height - 1, 0xff_F3EFE0, 0xff_F3EFE0);
            fillGradient(poseStack, this.x, this.y + this.height - 1, this.x + this.width, this.y + this.height, 0xff_F3EFE0, 0xff_F3EFE0);
        }
        this.renderScrollingString(poseStack, font, 2, 0xF3EFE0);
    }

    public void setSelect(boolean select) {
        isSelect = select;
    }

    protected static void renderScrollingString(PoseStack poseStack, Font font, Component component, int pMinX, int pMinY, int pMaxX, int pMaxY, int pColor) {
        int fontWidth = font.width(component);
        int yOffset = (pMinY + pMaxY - 9) / 2 + 1;
        int showWidth = pMaxX - pMinX;
        if (fontWidth > showWidth) {
            int diff = fontWidth - showWidth;
            double i = (double) Util.getMillis() / 1000.0D;
            double j = Math.max((double) diff * 0.5D, 3.0D);
            double k = Math.sin((Math.PI / 2D) * Math.cos((Math.PI * 2D) * i / j)) / 2.0D + 0.5D;
            double xOffset = Mth.lerp(k, 0.0D, diff);
            enableSelfScissor(pMinX, pMinY, pMaxX - pMinX, pMaxY - pMinY);
            drawString(poseStack, font, component, pMinX - (int) xOffset, yOffset, pColor);
            RenderSystem.disableScissor();
        } else {
            drawCenteredString(poseStack, font, component, (pMinX + pMaxX) / 2, yOffset, pColor);
        }
    }

    protected void renderScrollingString(PoseStack poseStack, Font pFont, int width, int color) {
        int minX = this.x + width;
        int maxX = this.x + this.getWidth() - width;
        renderScrollingString(poseStack, pFont, this.getMessage(), minX, this.y, maxX, this.y + this.getHeight(), color);
    }

    private static void enableSelfScissor(int pX, int pY, int pWidth, int pHeight) {
        Window window = Minecraft.getInstance().getWindow();
        double guiScale = window.getGuiScale();
        int scissorX = (int) (pX * guiScale);
        int scissorY = (int) (window.getHeight() - ((pY + pHeight) * guiScale));
        int scissorW = (int) (pWidth * guiScale);
        int scissorH = (int) (pHeight * guiScale);
        RenderSystem.enableScissor(scissorX, scissorY, scissorW, scissorH);
    }
}
