package com.tacz.guns.client.gui.components.smith;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class ImageButton extends Button {
    private final int xTexStart;
    private final int yTexStart;
    private final int yDiffTex;
    private final ResourceLocation resourceLocation;

    public ImageButton(int pX, int pY, int pWidth, int pHeight, int pXTexStart, int pYTexStart, int pYDiffTex, ResourceLocation pResourceLocation, Button.OnPress pOnPress) {
        super(pX, pY, pWidth, pHeight, CommonComponents.EMPTY, pOnPress, DEFAULT_NARRATION);
        xTexStart = pXTexStart;
        yTexStart = pYTexStart;
        yDiffTex = pYDiffTex;
        resourceLocation = pResourceLocation;
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics gui, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderTexture(gui, this.resourceLocation, this.getX(), this.getY(), this.xTexStart, this.yTexStart, this.yDiffTex, this.width, this.height, 256, 256);
    }

    private void renderTexture(GuiGraphics pGuiGraphics, ResourceLocation pTexture, int pX, int pY, int pUOffset, int pVOffset, int pTextureDifference, int pWidth, int pHeight, int pTextureWidth, int pTextureHeight) {
        int i = pVOffset;
        if (!this.isActive()) {
            i = pVOffset + pTextureDifference * 2;
        } else if (this.isHoveredOrFocused()) {
            i = pVOffset + pTextureDifference;
        }

        RenderSystem.enableDepthTest();
        pGuiGraphics.blit(pTexture, pX, pY, (float)pUOffset, (float)i, pWidth, pHeight, pTextureWidth, pTextureHeight);
    }
}
