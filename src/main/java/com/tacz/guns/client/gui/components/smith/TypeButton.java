package com.tacz.guns.client.gui.components.smith;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.GunMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class TypeButton extends Button {
    private static final ResourceLocation TEXTURE = new ResourceLocation(GunMod.MOD_ID, "textures/gui/gun_smith_table.png");
    private final ItemStack stack;
    private boolean isSelected = false;

    public TypeButton(int pX, int pY, ItemStack stack, Button.OnPress onPress) {
        super(pX, pY, 24, 25, TextComponent.EMPTY, onPress);
        this.stack = stack;
    }

    @Override
    public void renderButton(PoseStack poseStack, int pMouseX, int pMouseY, float pPartialTick) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, TEXTURE);
        RenderSystem.enableDepthTest();
        if (isSelected) {
            int vOffset = isHoveredOrFocused() ? 204 + this.height : 204;
            blit(poseStack, this.x, this.y, 0, vOffset, this.width, this.height, 256, 256);
        } else {
            int vOffset = isHoveredOrFocused() ? 204 + this.height : 204;
            blit(poseStack, this.x, this.y, 26, vOffset, this.width, this.height, 256, 256);
        }
        Minecraft mc = Minecraft.getInstance();
        mc.getItemRenderer().renderGuiItem(this.stack, this.x + 4, this.y + 5);
    }

    @Override
    public void onPress() {
        this.isSelected = true;
        this.onPress.onPress(this);
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}
