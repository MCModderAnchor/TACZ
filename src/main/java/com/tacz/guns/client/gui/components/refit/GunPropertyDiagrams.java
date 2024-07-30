package com.tacz.guns.client.gui.components.refit;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.gui.GunRefitScreen;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import com.tacz.guns.resource.pojo.data.gun.*;
import com.tacz.guns.util.AttachmentDataUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedList;

public final class GunPropertyDiagrams {
    public static int getHidePropertyButtonYOffset() {
        int[] startYOffset = new int[]{49};
        AttachmentPropertyManager.getModifiers().forEach((key, value) -> {
            startYOffset[0] += value.getDiagramsDataSize() * 10;
        });
        return startYOffset[0];
    }

    public static void draw(GunRefitScreen screen, PoseStack poseStack, Font font, int x, int y) {
        Screen.fill(poseStack, x, y, x + 288, y + getHidePropertyButtonYOffset() - 11, 0xAF222222);

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
            FireMode fireMode = iGun.getFireMode(gunItem);

            int barStartX = x + 83;
            int barMaxWidth = 120;
            int barEndX = barStartX + barMaxWidth;

            int barBackgroundColor = 0xFF000000;
            int barBaseColor = 0xFFFFFFFF;
            int barPositivelyColor = 0xFF_55FF55;
            int barNegativeColor = 0xFF_FF5555;

            int fontColor = 0xCCCCCC;
            int nameTextStartX = x + 5;
            int valueTextStartX = x + 210;

            int[] yOffset = new int[]{y + 5};

            // 射击模式
            TranslatableComponent fireModeText = new TranslatableComponent("gui.tacz.gun_refit.property_diagrams.fire_mode");
            if (fireMode == FireMode.AUTO) {
                fireModeText.append(new TranslatableComponent("gui.tacz.gun_refit.property_diagrams.auto"));
            } else if (fireMode == FireMode.SEMI) {
                fireModeText.append(new TranslatableComponent("gui.tacz.gun_refit.property_diagrams.semi"));
            } else if (fireMode == FireMode.BURST) {
                fireModeText.append(new TranslatableComponent("gui.tacz.gun_refit.property_diagrams.burst"));
            } else {
                fireModeText.append(new TranslatableComponent("gui.tacz.gun_refit.property_diagrams.unknown"));
            }

            font.draw(poseStack, fireModeText, nameTextStartX, yOffset[0], fontColor);

            yOffset[0] += 10;


            // 弹匣容量
            int barrelBulletAmount = (iGun.hasBulletInBarrel(gunItem) && index.getGunData().getBolt() != Bolt.OPEN_BOLT) ? 1 : 0;
            int ammoAmount = gunData.getAmmoAmount() + barrelBulletAmount;
            double ammoAmountPercent = Math.min(ammoAmount / 100.0, 1);
            int ammoLength = (int) (barStartX + barMaxWidth * ammoAmountPercent);
            int maxAmmoCount = AttachmentDataUtils.getAmmoCountWithAttachment(gunItem, index.getGunData()) + barrelBulletAmount;
            int addAmmoCount = Math.max(maxAmmoCount - ammoAmount, 0);
            int addAmmoCountLength = (int) (barMaxWidth * addAmmoCount / 100.0);

            font.draw(poseStack, new TranslatableComponent("gui.tacz.gun_refit.property_diagrams.ammo_capacity"), nameTextStartX, yOffset[0], fontColor);
            Screen.fill(poseStack, barStartX, yOffset[0] + 2, barEndX, yOffset[0] + 6, barBackgroundColor);
            Screen.fill(poseStack, barStartX, yOffset[0] + 2, ammoLength, yOffset[0] + 6, barBaseColor);
            if (addAmmoCount > 0) {
                int barRight = Math.min(ammoLength + addAmmoCountLength, barEndX);
                Screen.fill(poseStack, ammoLength, yOffset[0] + 2, barRight, yOffset[0] + 6, barPositivelyColor);
                font.draw(poseStack, String.format("%d §a(+%d)", ammoAmount, addAmmoCount), valueTextStartX, yOffset[0], fontColor);
            } else {
                font.draw(poseStack, String.format("%d", ammoAmount), valueTextStartX, yOffset[0], fontColor);
            }

            yOffset[0] += 10;


            // 跑射延迟
            float aimTime = gunData.getAimTime();
            float sprintTime = gunData.getSprintTime();
            double sprintTimePercent = Mth.clamp(aimTime, 0, 1);
            int sprintLength = (int) (barStartX + barMaxWidth * sprintTimePercent);
            String sprintValueText = String.format("%.2fs", sprintTime);

            font.draw(poseStack, new TranslatableComponent("gui.tacz.gun_refit.property_diagrams.sprint_time"), nameTextStartX, yOffset[0], fontColor);
            Screen.fill(poseStack, barStartX, yOffset[0] + 2, barEndX, yOffset[0] + 6, barBackgroundColor);
            Screen.fill(poseStack, barStartX, yOffset[0] + 2, sprintLength, yOffset[0] + 6, barBaseColor);
            font.draw(poseStack, sprintValueText, valueTextStartX, yOffset[0], fontColor);

            yOffset[0] += 10;

            AttachmentPropertyManager.getModifiers().forEach((key, value) -> value.getPropertyDiagramsData(gunItem, gunData, cacheProperty).forEach(data -> {
                double defaultPercent = data.defaultPercent();
                double modifierPercent = data.modifierPercent();
                double modifier = data.modifier().doubleValue();
                String titleKey = data.titleKey();
                String positivelyString = data.positivelyString();
                String negativeString = data.negativeString();
                String defaultString = data.defaultString();
                boolean positivelyBetter = data.positivelyBetter();

                defaultPercent = Mth.clamp(defaultPercent, 0, 1);
                int defaultLength = (int) (barStartX + barMaxWidth * defaultPercent);
                int modifierLength = Mth.clamp(defaultLength + (int) (barMaxWidth * modifierPercent), barStartX, barEndX);

                font.draw(poseStack, Component.translatable(titleKey), nameTextStartX, yOffset[0], fontColor);
                Screen.fill(poseStack, barStartX, yOffset[0] + 2, barEndX, yOffset[0] + 6, barBackgroundColor);
                Screen.fill(poseStack, barStartX, yOffset[0] + 2, defaultLength, yOffset[0] + 6, barBaseColor);
                if (modifier > 0) {
                    int barColor = positivelyBetter ? barPositivelyColor : barNegativeColor;
                    Screen.fill(poseStack, defaultLength, yOffset[0] + 2, modifierLength, yOffset[0] + 6, barColor);
                    font.draw(poseStack, positivelyString, valueTextStartX, yOffset[0], fontColor);
                } else if (modifier < 0) {
                    int barColor = positivelyBetter ? barNegativeColor : barPositivelyColor;
                    Screen.fill(poseStack, modifierLength, yOffset[0] + 2, defaultLength, yOffset[0] + 6, barColor);
                    font.draw(poseStack, negativeString, valueTextStartX, yOffset[0], fontColor);
                } else {
                    font.draw(poseStack, defaultString, valueTextStartX, yOffset[0], fontColor);
                }
                yOffset[0] += 10;
            }));
        });
    }
}
