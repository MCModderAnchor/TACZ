package com.tacz.guns.client.gui.components.smith;

import com.mojang.blaze3d.systems.RenderSystem;
import com.tacz.guns.GunMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class ResultButton extends Button {
    private static final ResourceLocation TEXTURE = new ResourceLocation(GunMod.MOD_ID, "textures/gui/gun_smith_table.png");
    private final ItemStack stack;
    private boolean isSelected = false;

    public ResultButton(int pX, int pY, ItemStack stack, Button.OnPress onPress) {
        super(pX, pY, 94, 16, Component.empty(), onPress, DEFAULT_NARRATION);
        this.stack = stack;
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics gui, int pMouseX, int pMouseY, float pPartialTick) {
        RenderSystem.enableDepthTest();

        if (isSelected) {
            if (isHoveredOrFocused()) {
                gui.blit(TEXTURE, this.getX() - 1, this.getY() - 1, 52, 229, this.width + 2, this.height + 2, 256, 256);
            } else {
                gui.blit(TEXTURE, this.getX(), this.getY(), 53, 230, this.width, this.height, 256, 256);
            }
        } else {
            if (isHoveredOrFocused()) {
                gui.blit(TEXTURE, this.getX() - 1, this.getY() - 1, 52, 211, this.width + 2, this.height + 2, 256, 256);
            } else {
                gui.blit(TEXTURE, this.getX(), this.getY(), 53, 212, this.width, this.height, 256, 256);
            }
        }
        Minecraft mc = Minecraft.getInstance();
        gui.renderItem(stack, this.getX() + 1, this.getY());

        Component hoverName = this.stack.getHoverName();
        renderScrollingString(gui, mc.font, hoverName, this.getX() + 20, this.getY() + 4, this.getX() + 92, this.getY() + 13, 0xFFFFFF);
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
        if (this.isHoveredOrFocused() && !this.stack.isEmpty()) {
            consumer.accept(this.stack);
        }
    }
}
