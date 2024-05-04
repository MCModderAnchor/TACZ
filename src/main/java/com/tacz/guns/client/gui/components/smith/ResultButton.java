package com.tacz.guns.client.gui.components.smith;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.GunMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.List;
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
        List<FormattedCharSequence> split = mc.font.split(hoverName, 70);
        mc.font.draw(poseStack, split.get(0), this.x + 22, this.y + 4, 0xFFFFFF);
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
}
