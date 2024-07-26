package com.tacz.guns.client.gui.components.refit;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.config.sync.SyncConfig;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import com.tacz.guns.resource.pojo.data.gun.*;
import com.tacz.guns.util.AttachmentDataUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedList;

public final class GunPropertyDiagrams {
    public static int getHidePropertyButtonYOffset() {
        int[] startYOffset = new int[]{79};
        AttachmentPropertyManager.getModifiers().forEach((key, value) -> {
            startYOffset[0] += value.getYOffset();
        });
        return startYOffset[0];
    }

    public static void draw(GuiGraphics graphics, Font font, int x, int y) {
        graphics.fill(x, y, x + 258, y + 118, 0xAF222222);

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        ItemStack gunItem = player.getMainHandItem();
        IGun iGun = IGun.getIGunOrNull(gunItem);
        if (iGun == null) {
            return;
        }
        AttachmentCacheProperty cacheProperty = IGunOperator.fromLivingEntity(player).getCacheProperty();
        if (cacheProperty == null) {
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

            int[] yOffset = new int[]{y + 5};

            // 射击模式
            MutableComponent fireModeText = Component.translatable("gui.tacz.gun_refit.property_diagrams.fire_mode");
            if (fireMode == FireMode.AUTO) {
                fireModeText.append(Component.translatable("gui.tacz.gun_refit.property_diagrams.auto"));
            } else if (fireMode == FireMode.SEMI) {
                fireModeText.append(Component.translatable("gui.tacz.gun_refit.property_diagrams.semi"));
            } else if (fireMode == FireMode.BURST) {
                fireModeText.append(Component.translatable("gui.tacz.gun_refit.property_diagrams.burst"));
            } else {
                fireModeText.append(Component.translatable("gui.tacz.gun_refit.property_diagrams.unknown"));
            }

            graphics.drawString(font, fireModeText, nameTextStartX, yOffset[0], fontColor, false);

            yOffset[0] += 10;


            // 伤害
            double damageAmount = bulletData.getDamageAmount();
            if (fireModeAdjustData != null) {
                damageAmount += fireModeAdjustData.getDamageAmount();
            }
            damageAmount = Math.max(damageAmount * SyncConfig.DAMAGE_BASE_MULTIPLIER.get(), 0F);
            double damagePercent = Math.min(Math.log(damageAmount) / 5.0, 1);
            int damageLength = (int) (barStartX + barMaxWidth * damagePercent);
            String damageValueText = String.format("%.2f", damageAmount);

            graphics.drawString(font, Component.translatable("gui.tacz.gun_refit.property_diagrams.damage"), nameTextStartX, yOffset[0], fontColor, false);
            graphics.fill(barStartX, yOffset[0] + 2, barEndX, yOffset[0] + 6, barBackgroundColor);
            graphics.fill(barStartX, yOffset[0] + 2, damageLength, yOffset[0] + 6, barBaseColor);
            graphics.drawString(font, damageValueText, valueTextStartX, yOffset[0], fontColor, false);

            yOffset[0] += 10;


            // 精确度，也就是瞄准时的扩散
            float aimInaccuracy = gunData.getInaccuracy(InaccuracyType.AIM);
            if (fireModeAdjustData != null) {
                aimInaccuracy += fireModeAdjustData.getAimInaccuracy();
            }
            double aimInaccuracyPercent = Mth.clamp(1 - aimInaccuracy, 0, 1);
            int aimInaccuracyLength = (int) (barStartX + barMaxWidth * aimInaccuracyPercent);
            String aimInaccuracyValueText = String.format("%.2f%%", aimInaccuracyPercent * 100);

            graphics.drawString(font, Component.translatable("gui.tacz.gun_refit.property_diagrams.accuracy"), nameTextStartX, yOffset[0], fontColor, false);
            graphics.fill(barStartX, yOffset[0] + 2, barEndX, yOffset[0] + 6, barBackgroundColor);
            graphics.fill(barStartX, yOffset[0] + 2, aimInaccuracyLength, yOffset[0] + 6, barBaseColor);
            graphics.drawString(font, aimInaccuracyValueText, valueTextStartX, yOffset[0], fontColor, false);

            yOffset[0] += 10;


            // 优势射程
            ExtraDamage extraDamage = bulletData.getExtraDamage();
            float effectiveRange = 0f;
            if (extraDamage != null) {
                LinkedList<ExtraDamage.DistanceDamagePair> damageDecay = extraDamage.getDamageAdjust();
                if (damageDecay.size() > 0) {
                    effectiveRange = damageDecay.get(0).getDistance();
                }
            }
            double effectiveRangePercent = Mth.clamp(effectiveRange / 100.0, 0, 1);
            int effectiveRangeLength = (int) (barStartX + barMaxWidth * effectiveRangePercent);
            String effectiveRangeValueText = String.format("%.1fm", effectiveRange);

            graphics.drawString(font, Component.translatable("gui.tacz.gun_refit.property_diagrams.effective_range"), nameTextStartX, yOffset[0], fontColor, false);
            graphics.fill(barStartX, yOffset[0] + 2, barEndX, yOffset[0] + 6, barBackgroundColor);
            graphics.fill(barStartX, yOffset[0] + 2, effectiveRangeLength, yOffset[0] + 6, barBaseColor);
            graphics.drawString(font, effectiveRangeValueText, valueTextStartX, yOffset[0], fontColor, false);

            yOffset[0] += 10;


            // 弹匣容量
            int barrelBulletAmount = (iGun.hasBulletInBarrel(gunItem) && index.getGunData().getBolt() != Bolt.OPEN_BOLT) ? 1 : 0;
            int ammoAmount = gunData.getAmmoAmount() + barrelBulletAmount;
            double ammoAmountPercent = Math.min(ammoAmount / 100.0, 1);
            int ammoLength = (int) (barStartX + barMaxWidth * ammoAmountPercent);
            int maxAmmoCount = AttachmentDataUtils.getAmmoCountWithAttachment(gunItem, index.getGunData()) + barrelBulletAmount;
            int addAmmoCount = Math.max(maxAmmoCount - ammoAmount, 0);
            int addAmmoCountLength = (int) (barMaxWidth * addAmmoCount / 100.0);

            graphics.drawString(font, Component.translatable("gui.tacz.gun_refit.property_diagrams.ammo_capacity"), nameTextStartX, yOffset[0], fontColor, false);
            graphics.fill(barStartX, yOffset[0] + 2, barEndX, yOffset[0] + 6, barBackgroundColor);
            graphics.fill(barStartX, yOffset[0] + 2, ammoLength, yOffset[0] + 6, barBaseColor);
            if (addAmmoCount > 0) {
                int barRight = Math.min(ammoLength + addAmmoCountLength, barEndX);
                graphics.fill(ammoLength, yOffset[0] + 2, barRight, yOffset[0] + 6, barPositivelyColor);
                graphics.drawString(font, String.format("%d §a(+%d)", ammoAmount, addAmmoCount), valueTextStartX, yOffset[0], fontColor, false);
            } else {
                graphics.drawString(font, String.format("%d", ammoAmount), valueTextStartX, yOffset[0], fontColor, false);
            }

            yOffset[0] += 10;


            // 跑射延迟
            float aimTime = gunData.getAimTime();
            float sprintTime = gunData.getSprintTime();
            double sprintTimePercent = Mth.clamp(aimTime, 0, 1);
            int sprintLength = (int) (barStartX + barMaxWidth * sprintTimePercent);
            String sprintValueText = String.format("%.2fs", sprintTime);

            graphics.drawString(font, Component.translatable("gui.tacz.gun_refit.property_diagrams.sprint_time"), nameTextStartX, yOffset[0], fontColor, false);
            graphics.fill(barStartX, yOffset[0] + 2, barEndX, yOffset[0] + 6, barBackgroundColor);
            graphics.fill(barStartX, yOffset[0] + 2, sprintLength, yOffset[0] + 6, barBaseColor);
            graphics.drawString(font, sprintValueText, valueTextStartX, yOffset[0], fontColor, false);

            yOffset[0] += 10;

            AttachmentPropertyManager.getModifiers().forEach((key, value) -> {
                value.renderPropertyDiagrams(gunItem, gunData, cacheProperty,
                        barStartX, barEndX, barMaxWidth,
                        barBackgroundColor, barBaseColor, barPositivelyColor, barNegativeColor,
                        fontColor, nameTextStartX, valueTextStartX,
                        graphics, font, yOffset[0]);
                yOffset[0] += value.getYOffset();
            });
        });
    }
}
