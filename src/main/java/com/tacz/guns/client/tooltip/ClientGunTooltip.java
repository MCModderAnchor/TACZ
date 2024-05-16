package com.tacz.guns.client.tooltip;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import com.tacz.guns.client.input.RefitKey;
import com.tacz.guns.client.resource.ClientAssetManager;
import com.tacz.guns.client.resource.pojo.CustomTabPOJO;
import com.tacz.guns.client.resource.pojo.PackInfo;
import com.tacz.guns.config.sync.SyncConfig;
import com.tacz.guns.inventory.tooltip.GunTooltip;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import com.tacz.guns.util.AttachmentDataUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class ClientGunTooltip implements ClientTooltipComponent {
    private final ItemStack gun;
    private final IGun iGun;
    private final CommonGunIndex gunIndex;
    private final ItemStack ammo;
    private Component ammoName;
    private MutableComponent ammoCountText;
    private @Nullable MutableComponent gunType;
    private MutableComponent damage;
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
        return 86;
    }

    @Override
    public int getWidth(Font font) {
        return this.maxWidth;
    }

    private void getText() {
        Font font = Minecraft.getInstance().font;

        this.ammoName = ammo.getHoverName();
        this.maxWidth = Math.max(font.width(this.ammoName) + 22, this.maxWidth);

        int barrelBulletAmount = (iGun.hasBulletInBarrel(gun) && gunIndex.getGunData().getBolt() != Bolt.OPEN_BOLT) ? 1 : 0;
        int maxAmmoCount = AttachmentDataUtils.getAmmoCountWithAttachment(gun, gunIndex.getGunData()) + barrelBulletAmount;
        int currentAmmoCount = iGun.getCurrentAmmoCount(this.gun) + barrelBulletAmount;
        this.ammoCountText = Component.literal("%d/%d".formatted(currentAmmoCount, maxAmmoCount));
        this.maxWidth = Math.max(font.width(this.ammoCountText) + 22, this.maxWidth);

        CustomTabPOJO tab = ClientAssetManager.INSTANCE.getAllCustomTabs().get(gunIndex.getType());
        if (tab != null) {
            this.gunType = Component.translatable("tooltip.tacz.gun.type").append(Component.translatable(tab.getNameKey()).withStyle(ChatFormatting.AQUA));
            this.maxWidth = Math.max(font.width(this.gunType), this.maxWidth);
        }

        MutableComponent value = Component.literal(String.valueOf(gunIndex.getBulletData().getDamageAmount() * SyncConfig.DAMAGE_BASE_MULTIPLIER.get())).withStyle(ChatFormatting.AQUA);
        this.damage = Component.translatable("tooltip.tacz.gun.damage").append(value);
        this.maxWidth = Math.max(font.width(this.damage), this.maxWidth);

        String keyName = Component.keybind(RefitKey.REFIT_KEY.getName()).getString().toUpperCase(Locale.ENGLISH);
        this.tips = Component.translatable("tooltip.tacz.gun.tips", keyName).withStyle(ChatFormatting.YELLOW).withStyle(ChatFormatting.ITALIC);
        this.maxWidth = Math.max(font.width(this.tips), this.maxWidth);

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

        ResourceLocation gunId = iGun.getGunId(gun);
        PackInfo packInfoObject = ClientAssetManager.INSTANCE.getPackInfo(gunId);
        if (packInfoObject != null) {
            packInfo = Component.translatable(packInfoObject.getName()).withStyle(ChatFormatting.BLUE).withStyle(ChatFormatting.ITALIC);
            this.maxWidth = Math.max(font.width(this.packInfo), this.maxWidth);
        }
    }

    @Override
    public void renderText(Font font, int pX, int pY, Matrix4f matrix4f, MultiBufferSource.BufferSource bufferSource) {
        // 弹药名
        font.drawInBatch(this.ammoName, pX + 20, pY + 2, 0xffaa00, false, matrix4f, bufferSource, false, 0, 0xF000F0);

        // 弹药数
        font.drawInBatch(this.ammoCountText, pX + 20, pY + 13, 0x777777, false, matrix4f, bufferSource, false, 0, 0xF000F0);

        int yOffset = pY + 27;

        // 等级信息
        font.drawInBatch(this.levelInfo, pX, yOffset, 0x777777, false, matrix4f, bufferSource, false, 0, 0xF000F0);
        yOffset += 11;

        // 枪械类型
        if (this.gunType != null) {
            font.drawInBatch(this.gunType, pX, yOffset, 0x777777, false, matrix4f, bufferSource, false, 0, 0xF000F0);
            yOffset += 11;
        }

        // 伤害
        font.drawInBatch(this.damage, pX, yOffset, 0x777777, false, matrix4f, bufferSource, false, 0, 0xF000F0);
        yOffset += 11;

        // Z 键说明
        font.drawInBatch(this.tips, pX, yOffset + 4, 0xffffff, false, matrix4f, bufferSource, false, 0, 0xF000F0);
        yOffset += 12;

        // 枪包名
        if (packInfo != null) {
            font.drawInBatch(this.packInfo, pX, yOffset + 4, 0xffffff, false, matrix4f, bufferSource, false, 0, 0xF000F0);
        }
    }

    @Override
    public void renderImage(Font font, int mouseX, int mouseY, PoseStack poseStack, ItemRenderer itemRenderer, int blitOffset) {
        IGun iGun = IGun.getIGunOrNull(this.gun);
        if (iGun == null) {
            return;
        }
        itemRenderer.renderGuiItem(ammo, mouseX, mouseY + 3);
    }
}
