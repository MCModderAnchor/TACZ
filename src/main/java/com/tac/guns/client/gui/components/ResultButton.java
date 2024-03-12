package com.tac.guns.client.gui.components;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tac.guns.GunMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

public class ResultButton extends Button {
    private static final ResourceLocation TEXTURE = new ResourceLocation(GunMod.MOD_ID, "textures/gui/gun_smith_table.png");
    private final ItemStack stack;
    private boolean isSelected = false;

    public ResultButton(int pX, int pY, ItemStack stack, Button.OnPress onPress) {
        super(pX, pY, 83, 16, TextComponent.EMPTY, onPress);
        this.stack = stack;
    }

    @Override
    public void renderButton(PoseStack poseStack, int pMouseX, int pMouseY, float pPartialTick) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, TEXTURE);
        RenderSystem.enableDepthTest();
        if (isSelected) {
            if (isHoveredOrFocused()) {
                blit(poseStack, this.x - 1, this.y - 1, 31, 164, this.width + 2, this.height + 2, 256, 256);
            } else {
                blit(poseStack, this.x, this.y, 32, 165, this.width, this.height, 256, 256);
            }
        } else {
            if (isHoveredOrFocused()) {
                blit(poseStack, this.x - 1, this.y - 1, 31, 143, this.width + 2, this.height + 2, 256, 256);
            } else {
                blit(poseStack, this.x, this.y, 32, 144, this.width, this.height, 256, 256);
            }
        }
        Minecraft.getInstance().getItemRenderer().renderGuiItem(this.stack, this.x + 1, this.y);
        Minecraft.getInstance().font.draw(poseStack, this.stack.getHoverName(), this.x + 22, this.y + 4, 0xFFFFFF);
    }

    @Override
    public void onPress() {
        this.isSelected = true;
        this.onPress.onPress(this);
    }

    public void renderTooltips(Consumer<ItemStack> consumer) {
        if (this.isHovered && !this.stack.isEmpty()) {
            consumer.accept(this.stack);
        }
    }
}
