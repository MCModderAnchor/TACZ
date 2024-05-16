package com.tacz.guns.compat.cloth.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.client.renderer.crosshair.CrosshairType;
import me.shedaniel.clothconfig2.gui.entries.DropdownBoxEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

public class CrosshairDropdown {
    public static DropdownBoxEntry.SelectionTopCellElement<CrosshairType> of(CrosshairType type) {
        return new DropdownBoxEntry.DefaultSelectionTopCellElement<>(type, name -> {
            for (CrosshairType crosshairType : CrosshairType.values()) {
                if (crosshairType.name().equals(name)) {
                    return crosshairType;
                }
            }
            return null;
        }, id -> new TextComponent(id.toString())) {
            @Override
            public void render(PoseStack poseStack, int mouseX, int mouseY, int x, int y, int width, int height, float delta) {
                this.textFieldWidget.x = x + 4;
                this.textFieldWidget.y = y + 6;
                this.textFieldWidget.setWidth(width - 4 - 20);
                this.textFieldWidget.setEditable(this.getParent().isEditable());
                this.textFieldWidget.setTextColor(this.getPreferredTextColor());
                this.textFieldWidget.render(poseStack, mouseX, mouseY, delta);

                ResourceLocation location = CrosshairType.getTextureLocation(this.value);
                RenderSystem.setShaderTexture(0, location);
                blit(poseStack, x + width - 18, y + 2, 0, 0, 16, 16, 16, 16);
            }
        };
    }

    public static DropdownBoxEntry.SelectionCellCreator<CrosshairType> of() {
        return new DropdownBoxEntry.DefaultSelectionCellCreator<>(i -> new TextComponent(i.name())) {
            @Override
            public DropdownBoxEntry.SelectionCellElement<CrosshairType> create(CrosshairType selection) {
                return new DropdownBoxEntry.DefaultSelectionCellElement<>(selection, this.toTextFunction) {
                    @Override
                    public void render(PoseStack matrices, int mouseX, int mouseY, int x, int y, int width, int height, float delta) {
                        this.rendering = true;
                        this.x = x;
                        this.y = y;
                        this.width = width;
                        this.height = height;
                        boolean isHover = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
                        if (isHover) {
                            fill(matrices, x + 1, y + 1, x + width - 1, y + height - 1, 0xff191919);
                        }
                        FormattedCharSequence text = this.toTextFunction.apply(this.r).getVisualOrderText();
                        int color = isHover ? 0xffffff : 0x888888;
                        Minecraft.getInstance().font.drawShadow(matrices, text, (float) (x + 6 + 18), (float) (y + 6), color);

                        ResourceLocation location = CrosshairType.getTextureLocation(this.r);
                        RenderSystem.setShaderTexture(0, location);
                        blit(matrices, x + 4, y + 2, 0, 0, 16, 16, 16, 16);
                    }
                };
            }

            @Override
            public int getCellHeight() {
                return 20;
            }

            @Override
            public int getCellWidth() {
                return 146;
            }

            @Override
            public int getDropBoxMaxHeight() {
                return this.getCellHeight() * CrosshairType.values().length;
            }
        };
    }
}
