package com.tacz.guns.client.gui.components.refit;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.gun.FireMode;
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

import java.util.LinkedList;

public final class GunPropertyDiagrams {
    public static void draw(GuiGraphics graphics, Font font, int x, int y) {
        graphics.fill(x, y, x + 258, y + 108, 0xAF222222);

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
            FireMode fireMode = iGun.getFireMode(gunItem);
            GunFireModeAdjustData fireModeAdjustData = gunData.getFireModeAdjustData(fireMode);

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

            int pitch = 5;

            // 伤害
            double damageAmount = bulletData.getDamageAmount();
            if (fireModeAdjustData != null) {
                damageAmount += fireModeAdjustData.getDamageAmount();
            }
            damageAmount = Math.max(damageAmount * SyncConfig.DAMAGE_BASE_MULTIPLIER.get(), 0F);
            double damagePercent = Math.min(Math.log(damageAmount) / 5.0, 1);
            int damageLength = (int) (barStartX + barMaxWidth * damagePercent);
            String damageValueText = String.format("%.2f", damageAmount);

            graphics.drawString(font, Component.translatable("gui.tacz.gun_refit.property_diagrams.damage"), nameTextStartX, y + pitch, fontColor, false);
            graphics.fill(barStartX, y + pitch + 2, barEndX, y + pitch + 6, barBackgroundColor);
            graphics.fill(barStartX, y + pitch + 2, damageLength, y + pitch + 6, barBaseColor);
            graphics.drawString(font, damageValueText, valueTextStartX, y + pitch, fontColor, false);

            pitch += 10;


            // 射速
            int rpm = gunData.getRoundsPerMinute(fireMode);
            double rpmPercent = Math.min(rpm / 1200.0, 1);
            int rpmLength = (int) (barStartX + barMaxWidth * rpmPercent);
            String rpmValueText = String.format("%drpm", rpm);

            graphics.drawString(font, Component.translatable("gui.tacz.gun_refit.property_diagrams.rpm"), nameTextStartX, y + pitch, fontColor, false);
            graphics.fill(barStartX, y + pitch + 2, barEndX, y + pitch + 6, barBackgroundColor);
            graphics.fill(barStartX, y + pitch + 2, rpmLength, y + pitch + 6, barBaseColor);
            graphics.drawString(font, rpmValueText, valueTextStartX, y + pitch, fontColor, false);

            pitch += 10;


            // 精确度，也就是瞄准时的扩散
            float aimInaccuracy = gunData.getInaccuracy(InaccuracyType.AIM);
            if (fireModeAdjustData != null) {
                aimInaccuracy += fireModeAdjustData.getAimInaccuracy();
            }
            double aimInaccuracyPercent = Mth.clamp(1 - aimInaccuracy, 0, 1);
            int aimInaccuracyLength = (int) (barStartX + barMaxWidth * aimInaccuracyPercent);
            String aimInaccuracyValueText = String.format("%.2f%%", aimInaccuracyPercent * 100);

            graphics.drawString(font, Component.translatable("gui.tacz.gun_refit.property_diagrams.accuracy"), nameTextStartX, y + pitch, fontColor, false);
            graphics.fill(barStartX, y + pitch + 2, barEndX, y + pitch + 6, barBackgroundColor);
            graphics.fill(barStartX, y + pitch + 2, aimInaccuracyLength, y + pitch + 6, barBaseColor);
            graphics.drawString(font, aimInaccuracyValueText, valueTextStartX, y + pitch, fontColor, false);

            pitch += 10;


            // 优势射程
            ExtraDamage extraDamage = bulletData.getExtraDamage();
            float effectiveRange = 0f;
            if (extraDamage != null) {
                LinkedList<ExtraDamage.DistanceDamagePair> damageDecay = extraDamage.getDamageAdjust();
                effectiveRange = damageDecay.getFirst().getDistance();
            }
            double effectiveRangePercent = Mth.clamp(effectiveRange / 100.0, 0, 1);
            int effectiveRangeLength = (int) (barStartX + barMaxWidth * effectiveRangePercent);
            String effectiveRangeValueText = String.format("%.1fm", effectiveRange);

            graphics.drawString(font, Component.translatable("gui.tacz.gun_refit.property_diagrams.effective_range"), nameTextStartX, y + pitch, fontColor, false);
            graphics.fill(barStartX, y + pitch + 2, barEndX, y + pitch + 6, barBackgroundColor);
            graphics.fill(barStartX, y + pitch + 2, effectiveRangeLength, y + pitch + 6, barBaseColor);
            graphics.drawString(font, effectiveRangeValueText, valueTextStartX, y + pitch, fontColor, false);

            pitch += 10;


            // 腰射扩散
            float standInaccuracy = gunData.getInaccuracy(InaccuracyType.STAND);
            if (fireModeAdjustData != null) {
                standInaccuracy += fireModeAdjustData.getOtherInaccuracy();
            }
            double standInaccuracyPercent = Math.min(standInaccuracy / 10.0, 1);
            int inaccuracyLength = (int) (barStartX + barMaxWidth * standInaccuracyPercent);

            float[] inaccuracyModifier = new float[]{0};
            AttachmentDataUtils.getAllAttachmentData(gunItem, gunData, attachmentData -> inaccuracyModifier[0] += attachmentData.getInaccuracyAddend());
            double attachmentInaccuracyPercent = Math.min(inaccuracyModifier[0] / 10.0, 1);
            int inaccuracyModifierLength = Mth.clamp(inaccuracyLength + (int) (barMaxWidth * attachmentInaccuracyPercent), barStartX, barEndX);

            graphics.drawString(font, Component.translatable("gui.tacz.gun_refit.property_diagrams.hipfire_inaccuracy"), nameTextStartX, y + pitch, fontColor, false);
            graphics.fill(barStartX, y + pitch + 2, barEndX, y + pitch + 6, barBackgroundColor);
            graphics.fill(barStartX, y + pitch + 2, inaccuracyLength, y + pitch + 6, barBaseColor);
            if (attachmentInaccuracyPercent < 0) {
                graphics.fill(inaccuracyModifierLength, y + pitch + 2, inaccuracyLength, y + pitch + 6, barPositivelyColor);
                graphics.drawString(font, String.format("%.2f §a(%.2f)", standInaccuracy, inaccuracyModifier[0]), valueTextStartX, y + pitch, fontColor, false);
            } else if (attachmentInaccuracyPercent > 0) {
                graphics.fill(inaccuracyLength, y + pitch + 2, inaccuracyModifierLength, y + pitch + 6, barNegativeColor);
                graphics.drawString(font, String.format("%.2f §c(+%.2f)", standInaccuracy, inaccuracyModifier[0]), valueTextStartX, y + pitch, fontColor, false);
            } else {
                graphics.drawString(font, String.format("%.2f", standInaccuracy), valueTextStartX, y + pitch, fontColor, false);
            }

            pitch += 10;


            // 弹匣容量
            int barrelBulletAmount = (iGun.hasBulletInBarrel(gunItem) && index.getGunData().getBolt() != Bolt.OPEN_BOLT) ? 1 : 0;
            int ammoAmount = gunData.getAmmoAmount() + barrelBulletAmount;
            double ammoAmountPercent = Math.min(ammoAmount / 100.0, 1);
            int ammoLength = (int) (barStartX + barMaxWidth * ammoAmountPercent);
            int maxAmmoCount = AttachmentDataUtils.getAmmoCountWithAttachment(gunItem, index.getGunData()) + barrelBulletAmount;
            int addAmmoCount = Math.max(maxAmmoCount - ammoAmount, 0);
            int addAmmoCountLength = (int) (barMaxWidth * addAmmoCount / 100.0);

            graphics.drawString(font, Component.translatable("gui.tacz.gun_refit.property_diagrams.ammo_capacity"), nameTextStartX, y + pitch, fontColor, false);
            graphics.fill(barStartX, y + pitch + 2, barEndX, y + pitch + 6, barBackgroundColor);
            graphics.fill(barStartX, y + pitch + 2, ammoLength, y + pitch + 6, barBaseColor);
            if (addAmmoCount > 0) {
                int barRight = Math.min(ammoLength + addAmmoCountLength, barEndX);
                graphics.fill(ammoLength, y + pitch + 2, barRight, y + pitch + 6, barPositivelyColor);
                graphics.drawString(font, String.format("%d §a(+%d)", ammoAmount, addAmmoCount), valueTextStartX, y + pitch, fontColor, false);
            } else {
                graphics.drawString(font, String.format("%d", ammoAmount), valueTextStartX, y + pitch, fontColor, false);
            }

            pitch += 10;


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

            graphics.drawString(font, Component.translatable("gui.tacz.gun_refit.property_diagrams.yaw"), nameTextStartX, y + pitch, fontColor, false);
            graphics.fill(barStartX, y + pitch + 2, barEndX, y + pitch + 6, barBackgroundColor);
            graphics.fill(barStartX, y + pitch + 2, yawLength, y + pitch + 6, barBaseColor);
            if (attachmentRecoilModifier[1] > 0) {
                graphics.fill(yawLength, y + pitch + 2, yawModifierLength, y + pitch + 6, barNegativeColor);
                graphics.drawString(font, String.format("%.2f §c(+%.2f)", yawRecoil, attachmentRecoilModifier[1]), valueTextStartX, y + pitch, fontColor, false);
            } else if (attachmentRecoilModifier[1] < 0) {
                graphics.fill(yawModifierLength, y + pitch + 2, yawLength, y + pitch + 6, barPositivelyColor);
                graphics.drawString(font, String.format("%.2f §a(%.2f)", yawRecoil, attachmentRecoilModifier[1]), valueTextStartX, y + pitch, fontColor, false);
            } else {
                graphics.drawString(font, String.format("%.2f", yawRecoil), valueTextStartX, y + pitch, fontColor, false);
            }

            pitch += 10;

            graphics.drawString(font, Component.translatable("gui.tacz.gun_refit.property_diagrams.pitch"), nameTextStartX, y + pitch, fontColor, false);
            graphics.fill(barStartX, y + pitch + 2, barEndX, y + pitch + 6, barBackgroundColor);
            graphics.fill(barStartX, y + pitch + 2, pitchLength, y + pitch + 6, barBaseColor);
            if (attachmentRecoilModifier[0] > 0) {
                graphics.fill(pitchLength, y + pitch + 2, pitchModifierLength, y + pitch + 6, barNegativeColor);
                graphics.drawString(font, String.format("%.2f §c(+%.2f)", pitchRecoil, attachmentRecoilModifier[0]), valueTextStartX, y + pitch, fontColor, false);
            } else if (attachmentRecoilModifier[0] < 0) {
                graphics.fill(pitchModifierLength, y + pitch + 2, pitchLength, y + pitch + 6, barPositivelyColor);
                graphics.drawString(font, String.format("%.2f §a(%.2f)", pitchRecoil, attachmentRecoilModifier[0]), valueTextStartX, y + pitch, fontColor, false);
            } else {
                graphics.drawString(font, String.format("%.2f", pitchRecoil), valueTextStartX, y + pitch, fontColor, false);
            }

            pitch += 10;


            // 开镜时间
            final float[] adsTimeModifier = new float[]{0f};
            AttachmentDataUtils.getAllAttachmentData(gunItem, gunData, attachmentData -> {
                adsTimeModifier[0] += attachmentData.getAdsAddendTime();
            });

            float aimTime = gunData.getAimTime();
            double aimTimePercent = Math.min(aimTime, 1);
            int aimeTimeLength = (int) (barStartX + barMaxWidth * aimTimePercent);
            int adsModifierLength = Mth.clamp(aimeTimeLength + (int) (barMaxWidth * adsTimeModifier[0]), barStartX, barEndX);

            graphics.drawString(font, Component.translatable("gui.tacz.gun_refit.property_diagrams.ads"), nameTextStartX, y + pitch, fontColor, false);
            graphics.fill(barStartX, y + pitch + 2, barEndX, y + pitch + 6, barBackgroundColor);
            graphics.fill(barStartX, y + pitch + 2, aimeTimeLength, y + pitch + 6, barBaseColor);
            if (adsTimeModifier[0] > 0) {
                graphics.fill(aimeTimeLength, y + pitch + 2, adsModifierLength, y + pitch + 6, barNegativeColor);
                graphics.drawString(font, String.format("%.2fs §c(+%.2f)", aimTime, adsTimeModifier[0]), valueTextStartX, y + pitch, fontColor, false);
            } else if (adsTimeModifier[0] < 0) {
                graphics.fill(adsModifierLength, y + pitch + 2, aimeTimeLength, y + pitch + 6, barPositivelyColor);
                graphics.drawString(font, String.format("%.2fs §a(%.2f)", aimTime, adsTimeModifier[0]), valueTextStartX, y + pitch, fontColor, false);
            } else {
                graphics.drawString(font, String.format("%.2fs", aimTime), valueTextStartX, y + pitch, fontColor, false);
            }

            pitch += 10;


            // 跑射延迟
            float sprintTime = gunData.getSprintTime();
            double sprintTimePercent = Mth.clamp(aimTime, 0, 1);
            int sprintLength = (int) (barStartX + barMaxWidth * sprintTimePercent);
            String sprintValueText = String.format("%.2fs", sprintTime);

            graphics.drawString(font, Component.translatable("gui.tacz.gun_refit.property_diagrams.sprint_time"), nameTextStartX, y + pitch, fontColor, false);
            graphics.fill(barStartX, y + pitch + 2, barEndX, y + pitch + 6, barBackgroundColor);
            graphics.fill(barStartX, y + pitch + 2, sprintLength, y + pitch + 6, barBaseColor);
            graphics.drawString(font, sprintValueText, valueTextStartX, y + pitch, fontColor, false);
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
