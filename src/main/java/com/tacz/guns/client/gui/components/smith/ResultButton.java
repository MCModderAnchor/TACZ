package com.tacz.guns.client.gui.components.smith;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.GunMod;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

public class ResultButton extends Button {
    private static final ResourceLocation TEXTURE = new ResourceLocation(GunMod.MOD_ID, "textures/gui/gun_smith_table.png");
    private final ItemStack stack;
    private boolean isSelected = false;

    public ResultButton(int pX, int pY, ItemStack stack, Button.OnPress onPress) {
        super(pX, pY, 94, 16, TextComponent.EMPTY, onPress);
        this.stack = stack;
    }

    @Override
    public void renderButton(PoseStack poseStack, int pMouseX, int pMouseY, float pPartialTick) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, TEXTURE);
        RenderSystem.enableDepthTest();
        if (isSelected) {
            if (isHoveredOrFocused()) {
                blit(poseStack, this.x - 1, this.y - 1, 52, 229, this.width + 2, this.height + 2, 256, 256);
            } else {
                blit(poseStack, this.x, this.y, 53, 230, this.width, this.height, 256, 256);
            }
        } else {
            if (isHoveredOrFocused()) {
                blit(poseStack, this.x - 1, this.y - 1, 52, 211, this.width + 2, this.height + 2, 256, 256);
            } else {
                blit(poseStack, this.x, this.y, 53, 212, this.width, this.height, 256, 256);
            }
        }
        Minecraft mc = Minecraft.getInstance();
        mc.getItemRenderer().renderGuiItem(this.stack, this.x + 1, this.y);
        Component hoverName = this.stack.getHoverName();
        renderScrollingString(poseStack, mc.font, hoverName, this.x + 20, this.y + 4, this.x + 92, this.y + 13, 0xFFFFFF);
    }

    @Override
    public void onPress() {
        this.isSelected = true;
        this.onPress.onPress(this);
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public void renderTooltips(Consumer<ItemStack> consumer) {
        if (this.isHovered && !this.stack.isEmpty()) {
            consumer.accept(this.stack);
        }
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
