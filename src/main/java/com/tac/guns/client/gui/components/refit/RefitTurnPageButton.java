package com.tac.guns.client.gui.components.refit;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tac.guns.client.gui.GunRefitScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

public class RefitTurnPageButton extends Button implements IComponentTooltip {
    private final boolean isUpPage;

    public RefitTurnPageButton(int pX, int pY, boolean isUpPage, OnPress pOnPress) {
        super(pX, pY, 18, 8, TextComponent.EMPTY, pOnPress);
        this.isUpPage = isUpPage;
    }

    @Override
    public void renderButton(@Nonnull PoseStack poseStack, int pMouseX, int pMouseY, float pPartialTick) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, GunRefitScreen.TURN_PAGE_TEXTURE);
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        int yOffset = isUpPage ? 0 : 80;
        if (isHoveredOrFocused()) {
            blit(poseStack, x, y, width, height, 0, yOffset, 180, 80, 180, 160);
        } else {
            blit(poseStack, x + 1, y + 1, width - 2, height - 2, 10, yOffset + 10, 180 - 20, 80 - 20, 180, 160);
        }
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    public boolean isUpPage() {
        return isUpPage;
    }

    @Override
    public void renderTooltip(Consumer<MutableComponent> consumer) {
        if (this.isHovered) {
            String key = isUpPage ? "tooltip.tac.page.previous" : "tooltip.tac.page.next";
            consumer.accept(new TranslatableComponent(key));
        }
    }
}
