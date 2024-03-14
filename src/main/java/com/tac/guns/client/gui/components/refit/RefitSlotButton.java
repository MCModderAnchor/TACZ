package com.tac.guns.client.gui.components.refit;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tac.guns.GunMod;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

public class RefitSlotButton extends Button {
    private static final ResourceLocation TEXTURE = new ResourceLocation(GunMod.MOD_ID, "textures/gui/refit_slot.png");
    private final ItemStack stack;
    private boolean isSelected = false;

    public RefitSlotButton(int pX, int pY, ItemStack stack, Button.OnPress onPress) {
        super(pX, pY, 18, 18, TextComponent.EMPTY, onPress);
        this.stack = stack;
    }

    @Override
    public void renderButton(@Nonnull PoseStack poseStack, int pMouseX, int pMouseY, float pPartialTick) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, TEXTURE);
        RenderSystem.enableDepthTest();
        if (isSelected || isHoveredOrFocused()) {
            blit(poseStack, x, y, 0, 0, width, height, 18, 18);
        }else {
            blit(poseStack, x + 1, y + 1, 1, 1, width - 2, height - 2, 18, 18);
        }
    }

    @Override
    public void onPress() {
        this.isSelected = true;
        this.onPress.onPress(this);
    }
}
