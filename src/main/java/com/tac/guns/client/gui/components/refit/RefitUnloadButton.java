package com.tac.guns.client.gui.components.refit;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tac.guns.client.gui.GunRefitScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.TextComponent;

import javax.annotation.Nonnull;

public class RefitUnloadButton extends Button {

    public RefitUnloadButton(int pX, int pY, Button.OnPress pOnPress) {
        super(pX, pY, 8, 8, TextComponent.EMPTY, pOnPress);
    }

    @Override
    public void renderButton(@Nonnull PoseStack poseStack, int pMouseX, int pMouseY, float pPartialTick) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, GunRefitScreen.UNLOAD_TEXTURE);
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        if (isHoveredOrFocused()) {
            blit(poseStack, x, y, width, height, 0, 0, 80, 80, 160, 80);
        } else {
            blit(poseStack, x, y, width, height, 80, 0, 80, 80, 160, 80);
        }
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }
}
