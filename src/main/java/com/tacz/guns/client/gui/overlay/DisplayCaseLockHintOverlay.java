package com.tacz.guns.client.gui.overlay;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

@net.neoforged.fml.common.EventBusSubscriber(value = Dist.CLIENT)
public class DisplayCaseLockHintOverlay implements net.minecraft.client.gui.LayeredDraw.Layer {
    private static final int DISPLAY_TICKS = 40;
    private static final int Y_OFFSET = 32;
    private static Component message = Component.empty();
    private static int ticksRemaining;

    public static void show(Component component) {
        message = component;
        ticksRemaining = DISPLAY_TICKS;
    }

    @Override
    public void render(GuiGraphics graphics, net.minecraft.client.DeltaTracker delta) {
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        if (ticksRemaining <= 0) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        graphics.drawString(font, message, (int) ((width - font.width(message)) / 2.0F), (int) (height / 2.0F + Y_OFFSET), ChatFormatting.YELLOW.getColor(), false);
    }

    @SubscribeEvent
    public static void onClientTick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        if (ticksRemaining > 0) {
            ticksRemaining--;
        }
    }
}
