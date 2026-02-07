package com.tacz.guns.client.gui.overlay;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.config.client.RenderConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;


public class AimlessZoomOverlay implements IGuiOverlay {
    private static long renderTimestamp = -1L;
    private static float zoom = 0;

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        if (!RenderConfig.AIMLESS_ZOOM_ENABLE.get()) {
            return;
        }
        int timeout = 4500;
        int fadeInTime = 100; // fade in in 0.1s
        double fadeOutTime = 3900; // fade out in 4.5 - 3.9 = 0.6s

        long remainTime = System.currentTimeMillis() - renderTimestamp;
        if (remainTime > timeout) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (!(player instanceof IClientPlayerGunOperator)) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof IGun)) {
            return;
        }

        Component text = Component.translatable(
                "gui.tacz.aimless_zoom",
                zoom
        );

        int alpha = 0xFF;

        // Face in
        if (remainTime < fadeInTime) {
            alpha = 0x0F + (int)((remainTime / (double)fadeInTime) * (0xFF - 0x0F));
        }
        // Fade out
        else if (remainTime > fadeOutTime) {
            alpha = 0xFF - (int)(((remainTime - fadeOutTime) / (double)(timeout - fadeOutTime)) * (0xFF - 0x0F));
        }

        alpha = Math.max(0x0F, Math.min(0xFF, alpha));
        int color = 0xFFFFFF | (alpha << 24);

        float targetX = width * 4f / 5f;
        float targetY = height * 7f / 12f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        PoseStack poseStack = graphics.pose();

        float desiredScale = 1.35f;
        float scale = desiredScale;

        int screenW = graphics.guiWidth();
        int screenH = graphics.guiHeight();

        int textW = mc.font.width(text);
        int textH = mc.font.lineHeight;

        float maxW = screenW - targetX;
        float maxH = screenH - targetY;

        float scaleW = maxW / textW;
        float scaleH = maxH / textH;
        scale = Math.min(scale, Math.min(scaleW, scaleH));
        scale = Math.max(scale, 0.5f);
        poseStack.pushPose();
        {
            poseStack.scale(scale, scale, 1.0f);
            graphics.drawString(
                    mc.font,
                    text,
                    (int)(targetX / scale),
                    (int)(targetY / scale),
                    color
            );
        }
        poseStack.popPose();
        RenderSystem.disableBlend();
    }

    public static void renderZoomChange(float Izoom) {
         renderTimestamp = System.currentTimeMillis();
         zoom = Izoom;
    }
}
