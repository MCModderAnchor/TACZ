package com.tacz.guns.client.gui.components.refit;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.config.sync.SyncConfig;
import com.tacz.guns.resource.pojo.data.attachment.RecoilModifier;
import com.tacz.guns.resource.pojo.data.gun.*;
import com.tacz.guns.util.AttachmentDataUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public final class GunPropertyDiagrams {
    public static void draw(GuiGraphics graphics, Font font, int x, int y) {
        graphics.fill(x, y, x + 258, y + 98, 0xAF222222);

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        ItemStack gunItem = player.getMainHandItem();
        IGun iGun = IGun.getIGunOrNull(gunItem);
        if (iGun == null) {
            return;
        }
        ResourceLocation gunId = iGun.getGunId(gunItem);
        TimelessAPI.getCommonGunIndex(gunId).ifPresent(index -> {
            GunData gunData = index.getGunData();
            BulletData bulletData = gunData.getBulletData();
            GunRecoil recoil = gunData.getRecoil();

            int barStartX = x + 58;
            int barMaxWidth = 120;
            int barEndX = barStartX + barMaxWidth;

            int barBackgroundColor = 0xFF000000;
            int barBaseColor = 0xFFFFFFFF;
            int barPositivelyColor = 0xFF_55FF55;
            int barNegativeColor = 0xFF_FF5555;

            int fontColor = 0xCCCCCC;
            int nameTextStartX = x + 5;
            int valueTextStartX = x + 185;

            // 伤害
            double damageAmount = bulletData.getDamageAmount() * SyncConfig.DAMAGE_BASE_MULTIPLIER.get();
            double damagePercent = Math.min(Math.log(damageAmount) / 5.0, 1);
            int damageLength = (int) (barStartX + barMaxWidth * damagePercent);
            String damageValueText = String.format("%.2f", damageAmount);

            graphics.drawString(font, Component.translatable("gui.tacz.gun_refit.property_diagrams.damage"), nameTextStartX, y + 5, fontColor, false);
            graphics.fill(barStartX, y + 7, barEndX, y + 11, barBackgroundColor);
            graphics.fill(barStartX, y + 7, damageLength, y + 11, barBaseColor);
            graphics.drawString(font, damageValueText, valueTextStartX, y + 5, fontColor, false);


            // 射速
            int rpm = gunData.getRoundsPerMinute();
            double rpmPercent = Math.min(rpm / 1200.0, 1);
            int rpmLength = (int) (barStartX + barMaxWidth * rpmPercent);
            String rpmValueText = String.format("%drpm", rpm);

            graphics.drawString(font, Component.translatable("gui.tacz.gun_refit.property_diagrams.rpm"), nameTextStartX, y + 15, fontColor, false);
            graphics.fill(barStartX, y + 17, barEndX, y + 21, barBackgroundColor);
            graphics.fill(barStartX, y + 17, rpmLength, y + 21, barBaseColor);
            graphics.drawString(font, rpmValueText, valueTextStartX, y + 15, fontColor, false);


            // 精确度，也就是瞄准时的扩散
            float aimInaccuracy = gunData.getInaccuracy(InaccuracyType.AIM);
            double aimInaccuracyPercent = Math.min(1 - aimInaccuracy / 5.0, 1);
            int aimInaccuracyLength = (int) (barStartX + barMaxWidth * aimInaccuracyPercent);
            String aimInaccuracyValueText = String.format("%.2f%%", aimInaccuracyPercent * 100);

            graphics.drawString(font, Component.translatable("gui.tacz.gun_refit.property_diagrams.accuracy"), nameTextStartX, y + 25, fontColor, false);
            graphics.fill(barStartX, y + 27, barEndX, y + 31, barBackgroundColor);
            graphics.fill(barStartX, y + 27, aimInaccuracyLength, y + 31, barBaseColor);
            graphics.drawString(font, aimInaccuracyValueText, valueTextStartX, y + 25, fontColor, false);

            // 腰射扩散
            float standInaccuracy = gunData.getInaccuracy(InaccuracyType.STAND);
            double standInaccuracyPercent = Math.min(standInaccuracy / 10.0, 1);
            int inaccuracyLength = (int) (barStartX + barMaxWidth * standInaccuracyPercent);

            float[] inaccuracyModifier = new float[]{0};
            AttachmentDataUtils.getAllAttachmentData(gunItem, gunData, attachmentData -> inaccuracyModifier[0] += attachmentData.getInaccuracyAddend());
            double attachmentInaccuracyPercent = Math.min(inaccuracyModifier[0] / 10.0, 1);
            int inaccuracyModifierLength = (int) (barMaxWidth * attachmentInaccuracyPercent);

            graphics.drawString(font, Component.translatable("gui.tacz.gun_refit.property_diagrams.hipfire_inaccuracy"), nameTextStartX, y + 35, fontColor, false);
            graphics.fill(barStartX, y + 37, barEndX, y + 41, barBackgroundColor);
            graphics.fill(barStartX, y + 37, inaccuracyLength, y + 41, barBaseColor);
            if (attachmentInaccuracyPercent < 0) {
                graphics.fill(inaccuracyLength + inaccuracyModifierLength, y + 37, inaccuracyLength, y + 41, barPositivelyColor);
                graphics.drawString(font, String.format("%.2f §a(%.2f)", standInaccuracy, inaccuracyModifier[0]), valueTextStartX, y + 35, fontColor, false);
            } else if (attachmentInaccuracyPercent > 0) {
                graphics.fill(inaccuracyLength, y + 37, inaccuracyLength + inaccuracyModifierLength, y + 41, barNegativeColor);
                graphics.drawString(font, String.format("%.2f §c(+%.2f)", standInaccuracy, inaccuracyModifier[0]), valueTextStartX, y + 35, fontColor, false);
            } else {
                graphics.drawString(font, String.format("%.2f", standInaccuracy), valueTextStartX, y + 35, fontColor, false);
            }


            // 弹匣容量
            int barrelBulletAmount = (iGun.hasBulletInBarrel(gunItem) && index.getGunData().getBolt() != Bolt.OPEN_BOLT) ? 1 : 0;
            int ammoAmount = gunData.getAmmoAmount() + barrelBulletAmount;
            double ammoAmountPercent = Math.min(ammoAmount / 100.0, 1);
            int ammoLength = (int) (barStartX + barMaxWidth * ammoAmountPercent);
            int maxAmmoCount = AttachmentDataUtils.getAmmoCountWithAttachment(gunItem, index.getGunData()) + barrelBulletAmount;
            int addAmmoCount = Math.max(maxAmmoCount - ammoAmount, 0);
            int addAmmoCountLength = (int) (barMaxWidth * addAmmoCount / 100.0);

            graphics.drawString(font, Component.translatable("gui.tacz.gun_refit.property_diagrams.ammo_capacity"), nameTextStartX, y + 45, fontColor, false);
            graphics.fill(barStartX, y + 47, barEndX, y + 51, barBackgroundColor);
            graphics.fill(barStartX, y + 47, ammoLength, y + 51, barBaseColor);
            if (addAmmoCount > 0) {
                graphics.fill(ammoLength, y + 47, ammoLength + addAmmoCountLength, y + 51, barPositivelyColor);
                graphics.drawString(font, String.format("%d §a(+%d)", ammoAmount, addAmmoCount), valueTextStartX, y + 45, fontColor, false);
            } else {
                graphics.drawString(font, String.valueOf(ammoAmount), valueTextStartX, y + 45, fontColor, false);
            }


            // 水平后坐力和垂直后坐力
            final float[] attachmentRecoilModifier = new float[]{0f, 0f};
            AttachmentDataUtils.getAllAttachmentData(gunItem, gunData, attachmentData -> {
                RecoilModifier recoilModifier = attachmentData.getRecoilModifier();
                if (recoilModifier == null) {
                    return;
                }
                attachmentRecoilModifier[0] += recoilModifier.getPitch();
                attachmentRecoilModifier[1] += recoilModifier.getYaw();
            });

            float yawRecoil = getMaxInGunRecoilKeyFrame(recoil.getYaw());
            double yawRecoilPercent = Math.min(yawRecoil / 5.0, 1);
            int yawLength = (int) (barStartX + barMaxWidth * yawRecoilPercent);
            int yawModifierLength = Mth.clamp(yawLength + (int) (barMaxWidth * attachmentRecoilModifier[1] / 5.0), barStartX, barEndX);

            float pitchRecoil = getMaxInGunRecoilKeyFrame(recoil.getPitch());
            double pitchRecoilPercent = Math.min(pitchRecoil / 5.0, 1);
            int pitchLength = (int) (barStartX + barMaxWidth * pitchRecoilPercent);
            int pitchModifierLength = Mth.clamp(pitchLength + (int) (barMaxWidth * attachmentRecoilModifier[0] / 5.0), barStartX, barEndX);

            graphics.drawString(font, Component.translatable("gui.tacz.gun_refit.property_diagrams.yaw"), nameTextStartX, y + 55, fontColor, false);
            graphics.fill(barStartX, y + 57, barEndX, y + 61, barBackgroundColor);
            graphics.fill(barStartX, y + 57, yawLength, y + 61, barBaseColor);
            if (attachmentRecoilModifier[1] > 0) {
                graphics.fill(yawLength, y + 57, yawModifierLength, y + 61, barNegativeColor);
                graphics.drawString(font, String.format("%.2f §c(+%.2f)", yawRecoil, attachmentRecoilModifier[1]), valueTextStartX, y + 55, fontColor, false);
            } else if (attachmentRecoilModifier[1] < 0) {
                graphics.fill(yawModifierLength, y + 57, yawLength, y + 61, barPositivelyColor);
                graphics.drawString(font, String.format("%.2f §a(%.2f)", yawRecoil, attachmentRecoilModifier[1]), valueTextStartX, y + 55, fontColor, false);
            } else {
                graphics.drawString(font, String.valueOf(yawRecoil), valueTextStartX, y + 55, fontColor, false);
            }

            graphics.drawString(font, Component.translatable("gui.tacz.gun_refit.property_diagrams.pitch"), nameTextStartX, y + 65, fontColor, false);
            graphics.fill(barStartX, y + 67, barEndX, y + 71, barBackgroundColor);
            graphics.fill(barStartX, y + 67, pitchLength, y + 71, barBaseColor);
            if (attachmentRecoilModifier[0] > 0) {
                graphics.fill(pitchLength, y + 67, pitchModifierLength, y + 71, barNegativeColor);
                graphics.drawString(font, String.format("%.2f §c(+%.2f)", pitchRecoil, attachmentRecoilModifier[0]), valueTextStartX, y + 65, fontColor, false);
            } else if (attachmentRecoilModifier[0] < 0) {
                graphics.fill(pitchModifierLength, y + 67, pitchLength, y + 71, barPositivelyColor);
                graphics.drawString(font, String.format("%.2f §a(%.2f)", pitchRecoil, attachmentRecoilModifier[0]), valueTextStartX, y + 65, fontColor, false);
            } else {
                graphics.drawString(font, String.valueOf(pitchRecoil), valueTextStartX, y + 65, fontColor, false);
            }


            // 开镜时间
            final float[] adsTimeModifier = new float[]{0f};
            AttachmentDataUtils.getAllAttachmentData(gunItem, gunData, attachmentData -> {
                adsTimeModifier[0] += attachmentData.getAdsAddendTime();
            });

            float aimTime = gunData.getAimTime();
            double aimTimePercent = Math.min(aimTime, 1);
            int aimeTimeLength = (int) (barStartX + barMaxWidth * aimTimePercent);
            int adsModifierLength = Mth.clamp(aimeTimeLength + (int) (barMaxWidth * adsTimeModifier[0]), barStartX, barEndX);

            graphics.drawString(font, Component.translatable("gui.tacz.gun_refit.property_diagrams.ads"), nameTextStartX, y + 75, fontColor, false);
            graphics.fill(barStartX, y + 77, barEndX, y + 81, barBackgroundColor);
            graphics.fill(barStartX, y + 77, aimeTimeLength, y + 81, barBaseColor);
            if (adsTimeModifier[0] > 0) {
                graphics.fill(aimeTimeLength, y + 77, adsModifierLength, y + 81, barNegativeColor);
                graphics.drawString(font, String.format("%.2fs §c(+%.2f)", pitchRecoil, adsTimeModifier[0]), valueTextStartX, y + 75, fontColor, false);
            } else if (adsTimeModifier[0] < 0) {
                graphics.fill(adsModifierLength, y + 77, aimeTimeLength, y + 81, barPositivelyColor);
                graphics.drawString(font, String.format("%.2fs §a(%.2f)", pitchRecoil, adsTimeModifier[0]), valueTextStartX, y + 75, fontColor, false);
            } else {
                graphics.drawString(font, String.valueOf(aimTime), valueTextStartX, y + 75, fontColor, false);
            }


            // 跑射延迟
            float sprintTime = gunData.getSprintTime();
            double sprintTimePercent = Math.min(aimTime, 1);
            int sprintLength = (int) (barStartX + barMaxWidth * sprintTimePercent);
            String sprintValueText = String.format("%.2fs", sprintTime);

            graphics.drawString(font, Component.translatable("gui.tacz.gun_refit.property_diagrams.sprint_time"), nameTextStartX, y + 85, fontColor, false);
            graphics.fill(barStartX, y + 87, barEndX, y + 91, barBackgroundColor);
            graphics.fill(barStartX, y + 87, sprintLength, y + 91, barBaseColor);
            graphics.drawString(font, sprintValueText, valueTextStartX, y + 85, fontColor, false);
        });
    }

    private static float getMaxInGunRecoilKeyFrame(GunRecoilKeyFrame[] frames) {
        if (frames.length == 0) {
            return 0;
        }
        float[] value = frames[0].getValue();
        float leftValue = Math.abs(value[0]);
        float rightValue = Math.abs(value[1]);
        return Math.max(leftValue, rightValue);
    }
}
