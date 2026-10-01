package com.tacz.guns.client.gui.overlay;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.index.ClientGunIndex;
import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.resource.pojo.data.gun.GunHeatData;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.ItemStack;

import java.text.DecimalFormat;

public class HeatBarOverlay implements LayeredDraw.Layer {
    private static final ResourceLocation HEATBASE = ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "textures/hud/heat_base.png");
    private static final DecimalFormat HEAT_FORMAT_PERCENT = new DecimalFormat("0.0%");
    private static float heatScale = 0.25f;

    @Override
    public void render(GuiGraphics graphics, DeltaTracker delta) {
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        if (!RenderConfig.GUN_HUD_ENABLE.get()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        int guiTicks = mc.gui.getGuiTicks();
        LocalPlayer player = mc.player;
        if (!(player instanceof IClientPlayerGunOperator) || player.isSpectator()) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof IGun iGun)) {
            return;
        }
        ResourceLocation gunId = iGun.getGunId(stack);
        GunData gunData = TimelessAPI.getClientGunIndex(gunId).map(ClientGunIndex::getGunData).orElse(null);
        GunDisplayInstance display = TimelessAPI.getGunDisplay(stack).orElse(null);
        if (gunData == null || display == null) {
            return;
        }

        PoseStack poseStack = graphics.pose();
        if(gunData.getHeatData() != null && iGun.hasHeatData(stack)) {
            poseStack.pushPose();
            GunHeatData heatData = gunData.getHeatData();
            float percent = iGun.getHeatAmount(stack) / heatData.getHeatMax();

            float scaleValue = ((iGun.getHeatAmount(stack) / heatData.getHeatMax()) / 8f) + 0.75f;

            if(heatScale < scaleValue) heatScale += 0.05f;
            if(heatScale > scaleValue) heatScale -= 0.025f;
            if(heatScale > scaleValue - 0.03 && heatScale < scaleValue + 0.055) heatScale = scaleValue;
            poseStack.scale(heatScale, heatScale, 1);

            boolean locked = iGun.isOverheatLocked(stack);

            renderOverheat(percent, graphics, (int) (width / heatScale), (int) (height / heatScale), locked, guiTicks);
            poseStack.popPose();
        }
    }

    public static void renderOverheat(float heatPercentage, GuiGraphics pGraphics, int w, int h,
                               boolean locked, int tickCount) {
        int barColor = getHeatColor(heatPercentage, locked, tickCount);
        pGraphics.fill(w / 2 - 30, h / 2 + 30, w / 2 - 30 + (int) (heatPercentage * 60), h / 2 + 34, barColor);
        if (locked) {
            if (tickCount % 20 < 10) {
                pGraphics.setColor(1, 0.1f, 0.1f, 1);
            } else {
                pGraphics.setColor(1, 1, 0.1f, 1);
            }
        }
        pGraphics.blit(HEATBASE, w / 2 - 64, h / 2 - 44, 0, 0, 128, 128, 128, 128);
        pGraphics.setColor(1, 1, 1, 1);

        Font font = Minecraft.getInstance().fontFilterFishy;
        String percentString = locked ? "!OVERHEAT!" : HEAT_FORMAT_PERCENT.format(heatPercentage);
        int color = locked ? (tickCount % 20 < 10 ? 0xFFFF0000 : 0xFFFFFF00) : 0xFFFFFFFF;

        pGraphics.drawString(font, percentString, w / 2 - (font.width(percentString) / 2), h / 2 + 38, color, true);
    }

    public static int getHeatColor(float percent, boolean locked, int tickCount) {
        if (locked) {
            return tickCount % 20 < 10 ? 0x9FFF0000 : 0x9FFFFF00;
        }
        if (percent < 0.4) return 0x9FFFFFFF;
        int color;
        if (percent <= 0.65) {
            color = FastColor.ARGB32.lerp(percent * 4 - 1.6f, 0x9FFFFFFF, 0x9FFFFF00);
        } else {
            color = FastColor.ARGB32.lerp((percent-0.65f) / 0.35f, 0x9FFFFF00, 0x9FFF0000);
        }
        return color;
    }
}
