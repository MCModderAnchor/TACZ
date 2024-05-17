package com.tacz.guns.client.gui.overlay;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.config.client.RenderConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class KillAmountOverlay implements IGuiOverlay {
    private static long killTimestamp = -1L;
    private static int killAmount = 0;

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        if (!RenderConfig.KILL_AMOUNT_ENABLE.get()) {
            return;
        }
        int timeout = (int) (RenderConfig.KILL_AMOUNT_DURATION_SECOND.get() * 1000);
        float colorCount = 30;

        long remainTime = System.currentTimeMillis() - killTimestamp;
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

        String text;
        if (killAmount < 10) {
            text = "\u2620 x 0" + killAmount;
        } else {
            text = "\u2620 x " + killAmount;
        }
        int fontWith = mc.font.width(text);
        double fadeOutTime = timeout / 3.0 * 2;
        float hue = (1 - Math.min((killAmount / colorCount), 1)) * 0.15f;
        int alpha = 0xFF;
        if (remainTime > fadeOutTime) {
            alpha = 0xFF - (int) ((remainTime - fadeOutTime) / (timeout - fadeOutTime) * 0xF0);
        }
        int color = Mth.hsvToRgb(hue, 0.75f, 1) + (alpha << 24);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        PoseStack poseStack = graphics.pose();

        poseStack.pushPose();
        {
            poseStack.scale(0.5f, 0.5f, 1);
            graphics.drawString(mc.font, text, (int) (width - fontWith / 2.0f), (height - 45) * 2 - 1, color);
        }
        poseStack.popPose();
        RenderSystem.disableBlend();
    }

    public static void markTimestamp() {
        int timeout = (int) (RenderConfig.KILL_AMOUNT_DURATION_SECOND.get() * 1000);
        if (System.currentTimeMillis() - killTimestamp > timeout) {
            killAmount = 0;
        }
        killTimestamp = System.currentTimeMillis();
        killAmount += 1;
    }
}
