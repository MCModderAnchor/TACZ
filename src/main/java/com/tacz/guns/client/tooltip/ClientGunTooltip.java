package com.tacz.guns.client.tooltip;

import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.client.input.RefitKey;
import com.tacz.guns.client.resource.ClientAssetManager;
import com.tacz.guns.client.resource.pojo.PackInfo;
import com.tacz.guns.config.sync.SyncConfig;
import com.tacz.guns.inventory.tooltip.GunTooltip;
import com.tacz.guns.item.GunTooltipPart;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.pojo.data.gun.*;
import com.tacz.guns.util.AttachmentDataUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;

public class ClientGunTooltip implements ClientTooltipComponent {
    private static final DecimalFormat FORMAT = new DecimalFormat("#.##%");
    private static final DecimalFormat FORMAT_P_D1 = new DecimalFormat("#.#%");
    private static final DecimalFormat DAMAGE_FORMAT = new DecimalFormat("#.##");

    private final ItemStack gun;
    private final IGun iGun;
    private final CommonGunIndex gunIndex;
    private final ItemStack ammo;
    private @Nullable List<FormattedCharSequence> desc;
    private Component ammoName;
    private MutableComponent ammoCountText;
    private @Nullable MutableComponent gunType;
    private MutableComponent damage;
    private MutableComponent armorIgnore;
    private MutableComponent headShotMultiplier;
    private MutableComponent weight;
    private MutableComponent tips;
    private MutableComponent levelInfo;
    private @Nullable MutableComponent packInfo;

    private int maxWidth;

    public ClientGunTooltip(GunTooltip tooltip) {
        this.gun = tooltip.getGun();
        this.iGun = tooltip.getIGun();
        ResourceLocation ammoId = tooltip.getAmmoId();
        this.gunIndex = tooltip.getGunIndex();
        this.ammo = AmmoItemBuilder.create().setId(ammoId).build();
        this.maxWidth = 0;
        this.getText();
    }

    @Override
    public int getHeight() {
        int height = 0;
        if (shouldShow(GunTooltipPart.DESCRIPTION) && this.desc != null) {
            height += 10 * this.desc.size() + 2;
        }
        if (shouldShow(GunTooltipPart.AMMO_INFO)) {
            height += 24;
        }
        if (shouldShow(GunTooltipPart.BASE_INFO)) {
            height += 34;
        }
        if (shouldShow(GunTooltipPart.EXTRA_DAMAGE_INFO)) {
            height += 34;
        }
        if (shouldShow(GunTooltipPart.UPGRADES_TIP)) {
            height += 14;
        }
        if (shouldShow(GunTooltipPart.PACK_INFO)) {
            height += 14;
        }
        return height;
    }

    @Override
    public int getWidth(Font font) {
        return this.maxWidth;
    }

    private void getText() {
        Font font = Minecraft.getInstance().font;
        BulletData bulletData = gunIndex.getBulletData();
        GunData gunData = gunIndex.getGunData();
        FireMode fireMode = iGun.getFireMode(gun);
        GunFireModeAdjustData fireModeAdjustData = gunData.getFireModeAdjustData(fireMode);

        if (shouldShow(GunTooltipPart.DESCRIPTION)) {
            @Nullable String tooltip = gunIndex.getPojo().getTooltip();
            if (tooltip != null) {
                List<FormattedCharSequence> split = font.split(Component.translatable(tooltip), 300);
                if (split.size() > 3) {
                    this.desc = split.subList(0, 3);
                } else {
                    this.desc = split;
                }
                for (FormattedCharSequence sequence : this.desc) {
                    this.maxWidth = Math.max(font.width(sequence), this.maxWidth);
                }
            }
        }


        if (shouldShow(GunTooltipPart.AMMO_INFO)) {
            this.ammoName = ammo.getHoverName();
            this.maxWidth = Math.max(font.width(this.ammoName) + 22, this.maxWidth);

            int barrelBulletAmount = (iGun.hasBulletInBarrel(gun) && gunIndex.getGunData().getBolt() != Bolt.OPEN_BOLT) ? 1 : 0;
            int maxAmmoCount = AttachmentDataUtils.getAmmoCountWithAttachment(gun, gunIndex.getGunData()) + barrelBulletAmount;
            int currentAmmoCount = iGun.getCurrentAmmoCount(this.gun) + barrelBulletAmount;

            if (!iGun.useDummyAmmo(gun)) {
                this.ammoCountText = Component.literal("%d/%d".formatted(currentAmmoCount, maxAmmoCount));
            } else {
                int dummyAmmoAmount = iGun.getDummyAmmoAmount(gun);
                this.ammoCountText = Component.literal("%d/%d (%d)".formatted(currentAmmoCount, maxAmmoCount, dummyAmmoAmount));
            }
            this.maxWidth = Math.max(font.width(this.ammoCountText) + 22, this.maxWidth);
        }


        if (shouldShow(GunTooltipPart.BASE_INFO)) {
            int expToNextLevel = iGun.getExpToNextLevel(gun);
            int expCurrentLevel = iGun.getExpCurrentLevel(gun);
            int level = iGun.getLevel(gun);
            if (level >= iGun.getMaxLevel()) {
                String levelText = String.format("%d (MAX)", level);
                this.levelInfo = Component.translatable("tooltip.tacz.gun.level").append(Component.literal(levelText).withStyle(ChatFormatting.DARK_PURPLE));
            } else {
                String levelText = String.format("%d (%.1f%%)", level, expCurrentLevel / (expToNextLevel + expCurrentLevel) * 100f);
                this.levelInfo = Component.translatable("tooltip.tacz.gun.level").append(Component.literal(levelText).withStyle(ChatFormatting.YELLOW));
            }
            this.maxWidth = Math.max(font.width(this.levelInfo), this.maxWidth);

            String tabKey = "tacz.type." + gunIndex.getType() + ".name";
            this.gunType = Component.translatable("tooltip.tacz.gun.type").append(Component.translatable(tabKey).withStyle(ChatFormatting.AQUA));
            this.maxWidth = Math.max(font.width(this.gunType), this.maxWidth);

            float damage = bulletData.getDamageAmount();
            if (fireModeAdjustData != null) {
                damage += fireModeAdjustData.getDamageAmount();
            }
            MutableComponent value = Component.literal(DAMAGE_FORMAT.format(damage * SyncConfig.DAMAGE_BASE_MULTIPLIER.get())).withStyle(ChatFormatting.AQUA);
            if (bulletData.getExplosionData() != null) {
                value.append(" + ").append(DAMAGE_FORMAT.format(bulletData.getExplosionData().getDamage() * SyncConfig.DAMAGE_BASE_MULTIPLIER.get())).append(Component.translatable("tooltip.tacz.gun.explosion"));
            }
            this.damage = Component.translatable("tooltip.tacz.gun.damage").append(value);
            this.maxWidth = Math.max(font.width(this.damage), this.maxWidth);
        }


        if (shouldShow(GunTooltipPart.EXTRA_DAMAGE_INFO)) {
            @Nullable ExtraDamage extraDamage = bulletData.getExtraDamage();
            if (extraDamage != null) {
                float armorIgnore = extraDamage.getArmorIgnore();
                if (fireModeAdjustData != null) {
                    armorIgnore += fireModeAdjustData.getArmorIgnore();
                }
                float armorDamagePercent = (float) (armorIgnore * SyncConfig.ARMOR_IGNORE_BASE_MULTIPLIER.get());

                float headShotMultiplier = extraDamage.getHeadShotMultiplier();
                if (fireModeAdjustData != null) {
                    headShotMultiplier += fireModeAdjustData.getHeadShotMultiplier();
                }
                float headShotMultiplierPercent = (float) (headShotMultiplier * SyncConfig.HEAD_SHOT_BASE_MULTIPLIER.get());

                armorDamagePercent = Mth.clamp(armorDamagePercent, 0.0F, 1.0F);
                this.armorIgnore = Component.translatable("tooltip.tacz.gun.armor_ignore", FORMAT.format(armorDamagePercent));
                this.headShotMultiplier = Component.translatable("tooltip.tacz.gun.head_shot_multiplier", FORMAT.format(headShotMultiplierPercent));
            } else {
                this.armorIgnore = Component.translatable("tooltip.tacz.gun.armor_ignore", FORMAT.format(0));
                this.headShotMultiplier = Component.translatable("tooltip.tacz.gun.head_shot_multiplier", FORMAT.format(1));
            }

            double weightFactor = SyncConfig.WEIGHT_SPEED_MULTIPLIER.get();
            double weight = AttachmentDataUtils.getWightWithAttachment(gun, gunData);
            this.weight = Component.translatable("tooltip.tacz.gun.movment_speed", FORMAT_P_D1.format(-weightFactor * weight)).withStyle(ChatFormatting.RED);

            this.maxWidth = Math.max(font.width(this.armorIgnore), this.maxWidth);
            this.maxWidth = Math.max(font.width(this.headShotMultiplier), this.maxWidth);
            this.maxWidth = Math.max(font.width(this.weight), this.maxWidth);
        }


        if (shouldShow(GunTooltipPart.UPGRADES_TIP)) {
            String keyName = Component.keybind(RefitKey.REFIT_KEY.getName()).getString().toUpperCase(Locale.ENGLISH);
            this.tips = Component.translatable("tooltip.tacz.gun.tips", keyName).withStyle(ChatFormatting.YELLOW).withStyle(ChatFormatting.ITALIC);
            this.maxWidth = Math.max(font.width(this.tips), this.maxWidth);
        }


        if (shouldShow(GunTooltipPart.PACK_INFO)) {
            ResourceLocation gunId = iGun.getGunId(gun);
            PackInfo packInfoObject = ClientAssetManager.INSTANCE.getPackInfo(gunId);
            if (packInfoObject != null) {
                packInfo = Component.translatable(packInfoObject.getName()).withStyle(ChatFormatting.BLUE).withStyle(ChatFormatting.ITALIC);
                this.maxWidth = Math.max(font.width(this.packInfo), this.maxWidth);
            }
        }
    }

    @Override
    public void renderText(Font font, int pX, int pY, Matrix4f matrix4f, MultiBufferSource.BufferSource bufferSource) {
        int yOffset = pY;

        if (shouldShow(GunTooltipPart.DESCRIPTION) && this.desc != null) {
            yOffset += 2;
            for (FormattedCharSequence sequence : this.desc) {
                font.drawInBatch(sequence, pX, yOffset, 0xaaaaaa, false, matrix4f, bufferSource, Font.DisplayMode.NORMAL, 0, 0xF000F0);
                yOffset += 10;
            }
        }


        if (shouldShow(GunTooltipPart.AMMO_INFO)) {
            yOffset += 4;

            // 弹药名
            font.drawInBatch(this.ammoName, pX + 20, yOffset, 0xffaa00, false, matrix4f, bufferSource, Font.DisplayMode.NORMAL, 0, 0xF000F0);

            // 弹药数
            font.drawInBatch(this.ammoCountText, pX + 20, yOffset + 10, 0x777777, false, matrix4f, bufferSource, Font.DisplayMode.NORMAL, 0, 0xF000F0);

            yOffset += 20;
        }


        if (shouldShow(GunTooltipPart.BASE_INFO)) {
            yOffset += 4;

            // 等级信息
            font.drawInBatch(this.levelInfo, pX, yOffset, 0x777777, false, matrix4f, bufferSource, Font.DisplayMode.NORMAL, 0, 0xF000F0);
            yOffset += 10;

            // 枪械类型
            if (this.gunType != null) {
                font.drawInBatch(this.gunType, pX, yOffset, 0x777777, false, matrix4f, bufferSource, Font.DisplayMode.NORMAL, 0, 0xF000F0);
                yOffset += 10;
            }

            // 伤害
            font.drawInBatch(this.damage, pX, yOffset, 0x777777, false, matrix4f, bufferSource, Font.DisplayMode.NORMAL, 0, 0xF000F0);
            yOffset += 10;
        }


        if (shouldShow(GunTooltipPart.EXTRA_DAMAGE_INFO)) {
            yOffset += 4;

            // 穿甲伤害
            font.drawInBatch(this.armorIgnore, pX, yOffset, 0xffaa00, false, matrix4f, bufferSource, Font.DisplayMode.NORMAL, 0, 0xF000F0);
            yOffset += 10;

            // 爆头伤害
            font.drawInBatch(this.headShotMultiplier, pX, yOffset, 0xffaa00, false, matrix4f, bufferSource, Font.DisplayMode.NORMAL, 0, 0xF000F0);
            yOffset += 10;

            font.drawInBatch(this.weight, pX, yOffset, 0xffffff, false, matrix4f, bufferSource, Font.DisplayMode.NORMAL, 0, 0xF000F0);
            yOffset += 10;
        }


        if (shouldShow(GunTooltipPart.UPGRADES_TIP)) {
            yOffset += 4;

            // Z 键说明
            font.drawInBatch(this.tips, pX, yOffset, 0xffffff, false, matrix4f, bufferSource, Font.DisplayMode.NORMAL, 0, 0xF000F0);
            yOffset += 10;
        }


        if (shouldShow(GunTooltipPart.PACK_INFO)) {
            // 枪包名
            if (packInfo != null) {
                yOffset += 4;
                font.drawInBatch(this.packInfo, pX, yOffset, 0xffffff, false, matrix4f, bufferSource, Font.DisplayMode.NORMAL, 0, 0xF000F0);
            }
        }
    }

    @Override
    public void renderImage(Font pFont, int pX, int pY, GuiGraphics guiGraphics) {
        IGun iGun = IGun.getIGunOrNull(this.gun);
        if (iGun == null) {
            return;
        }
        if (shouldShow(GunTooltipPart.AMMO_INFO)) {
            int yOffset = pY;
            if (shouldShow(GunTooltipPart.DESCRIPTION) && this.desc != null) {
                yOffset += this.desc.size() * 10 + 2;
            }
            guiGraphics.renderItem(ammo, pX, yOffset + 4);
        }
    }

    private boolean shouldShow(GunTooltipPart part) {
        return (GunTooltipPart.getHideFlags(this.gun) & part.getMask()) == 0;
    }
}
