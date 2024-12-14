package com.tacz.guns.client.tooltip;

import com.google.common.collect.Lists;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.client.resource.ClientAssetsManager;
import com.tacz.guns.client.resource.pojo.PackInfo;
import com.tacz.guns.inventory.tooltip.BlockItemTooltip;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.Arrays;
import java.util.List;

public class ClientBlockItemTooltip implements ClientTooltipComponent {
    private final ResourceLocation blockId;
    private final List<Component> components = Lists.newArrayList();
    private @Nullable MutableComponent packInfo;

    public ClientBlockItemTooltip(BlockItemTooltip tooltip) {
        this.blockId = tooltip.getBlockId();
        this.addText();
        this.addPackInfo();
    }

    private void addPackInfo() {
        PackInfo packInfoObject = ClientAssetsManager.INSTANCE.getPackInfo(blockId);
        if (packInfoObject != null) {
            packInfo = Component.translatable(packInfoObject.getName()).withStyle(ChatFormatting.BLUE).withStyle(ChatFormatting.ITALIC);
        }
    }


    @Override
    public int getHeight() {
        return components.size() * 10 + (packInfo != null ? 16 : 0);
    }

    @Override
    public int getWidth(Font font) {
        int[] width = new int[]{0};
        if (packInfo != null) {
            width[0] = Math.max(width[0], font.width(packInfo) + 4);
        }
        components.forEach(c -> width[0] = Math.max(width[0], font.width(c)));
        return width[0];
    }

    @Override
    public void renderText(Font font, int pX, int pY, Matrix4f matrix4f, MultiBufferSource.BufferSource bufferSource) {
        int yOffset = pY;
        for (Component component : this.components) {
            font.drawInBatch(component, pX, yOffset, 0xffaa00, false, matrix4f, bufferSource, Font.DisplayMode.NORMAL, 0, 0xF000F0);
            yOffset += 10;
        }
        // 枪包名
        if (packInfo != null) {
            font.drawInBatch(this.packInfo, pX, yOffset + 6, 0xffffff, false, matrix4f, bufferSource, Font.DisplayMode.NORMAL, 0, 0xF000F0);
        }
    }

    @Override
    public void renderImage(Font font, int mouseX, int mouseY, GuiGraphics gui) {
    }

    private void addText() {
        TimelessAPI.getClientBlockIndex(blockId).ifPresent(index -> {
            @Nullable String tooltipKey = index.getTooltipKey();
            if (tooltipKey != null) {
                String text = I18n.get(tooltipKey);
                String[] split = text.split("\n");
                Arrays.stream(split).forEach(s -> components.add(Component.literal(s).withStyle(ChatFormatting.GRAY)));
            }
        });
    }
}
