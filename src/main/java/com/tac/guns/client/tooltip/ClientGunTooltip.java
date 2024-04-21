package com.tac.guns.client.tooltip;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import com.tac.guns.api.item.IGun;
import com.tac.guns.client.input.RefitKey;
import com.tac.guns.client.resource.ClientAssetManager;
import com.tac.guns.client.resource.pojo.CustomTabPOJO;
import com.tac.guns.inventory.tooltip.GunTooltip;
import com.tac.guns.item.builder.AmmoItemBuilder;
import com.tac.guns.resource.index.CommonGunIndex;
import com.tac.guns.util.AttachmentDataUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class ClientGunTooltip implements ClientTooltipComponent {
    private final ItemStack gun;
    private final IGun iGun;
    private final ResourceLocation ammoId;
    private final CommonGunIndex gunIndex;
    private final ItemStack ammo;

    private Component ammoName;
    private MutableComponent ammoCountText;
    private @Nullable MutableComponent gunType;
    private MutableComponent damage;
    private MutableComponent tips;
    private MutableComponent ownerInfo;
    private MutableComponent levelInfo;

    private int maxWidth;

    public ClientGunTooltip(GunTooltip tooltip) {
        this.gun = tooltip.getGun();
        this.iGun = tooltip.getIGun();
        this.ammoId = tooltip.getAmmoId();
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


    String GUN_LEVEL_TAG = "GunLevel";
    String GUN_LEVEL_EXP_TAG = "GunLevelExp";
    String GUN_LEVEL_MAX_EXP_TAG = "GunLevelMaxExp";
    String GUN_LEVEL_LOCK_TAG = "GunLevelLock";
    String GUN_LEVEL_OWNER_TAG = "GunLevelOwner";
    private void getText() {
        Font font = Minecraft.getInstance().font;

        this.ammoName = ammo.getHoverName();
        this.maxWidth = Math.max(font.width(this.ammoName) + 22, this.maxWidth);

        int maxAmmoCount = AttachmentDataUtils.getAmmoCountWithAttachment(gun, gunIndex.getGunData());
        this.ammoCountText = new TextComponent("%d/%d".formatted(iGun.getCurrentAmmoCount(this.gun), maxAmmoCount));
        this.maxWidth = Math.max(font.width(this.ammoCountText) + 22, this.maxWidth);

        CustomTabPOJO tab = ClientAssetManager.INSTANCE.getAllCustomTabs().get(gunIndex.getType());
        if (tab != null) {
            this.gunType = new TranslatableComponent("tooltip.tac.gun.type").append(new TranslatableComponent(tab.getNameKey()).withStyle(ChatFormatting.AQUA));
            this.maxWidth = Math.max(font.width(this.gunType), this.maxWidth);
        }

        MutableComponent value = new TextComponent(String.valueOf(gunIndex.getBulletData().getDamageAmount())).withStyle(ChatFormatting.AQUA);
        this.damage = new TranslatableComponent("tooltip.tac.gun.damage").append(value);
        this.maxWidth = Math.max(font.width(this.damage), this.maxWidth);

        String keyName = new KeybindComponent(RefitKey.REFIT_KEY.getName()).getString().toUpperCase(Locale.ENGLISH);
        this.tips = new TranslatableComponent("tooltip.tac.gun.tips", keyName).withStyle(ChatFormatting.YELLOW).withStyle(ChatFormatting.ITALIC);
        this.maxWidth = Math.max(font.width(this.tips), this.maxWidth);

        CompoundTag nbt = this.gun.getOrCreateTag();
        String level = String.valueOf(nbt.getInt(GUN_LEVEL_TAG));
        float levelExp = nbt.getFloat(GUN_LEVEL_EXP_TAG);
        float levelMaxExp = nbt.getFloat(GUN_LEVEL_MAX_EXP_TAG);
        if (!nbt.contains(GUN_LEVEL_OWNER_TAG)) {
            this.ownerInfo = new TranslatableComponent("tooltip.tac.gun.level.no_owner").withStyle(ChatFormatting.DARK_GRAY);
        } else {
            String levelOwner = nbt.getString(GUN_LEVEL_OWNER_TAG);
            this.ownerInfo = new TranslatableComponent("tooltip.tac.gun.level.owner").append(new TranslatableComponent(levelOwner).withStyle(ChatFormatting.YELLOW));
        }
        boolean levelLock = nbt.getBoolean(GUN_LEVEL_LOCK_TAG);
        if (levelLock) {
            this.levelInfo = new TranslatableComponent("tooltip.tac.gun.level.error_owner").withStyle(ChatFormatting.RED);
        } else {
            if (nbt.getInt(GUN_LEVEL_TAG) >= 10) {
                this.levelInfo = new TranslatableComponent("tooltip.tac.gun.level").append(new TranslatableComponent(level).withStyle(ChatFormatting.YELLOW).append(" (MAX)"));
            } else {
                this.levelInfo = new TranslatableComponent("tooltip.tac.gun.level").append(new TranslatableComponent(level).withStyle(ChatFormatting.YELLOW).append(" (%.1f%%)".formatted(levelExp / levelMaxExp * 100)));
            }
        }
    }

    @Override
    public void renderText(Font font, int pX, int pY, Matrix4f matrix4f, MultiBufferSource.BufferSource bufferSource) {
        // 弹药名
        font.drawInBatch(this.ammoName, pX + 20, pY + 2, 0xffaa00, false, matrix4f, bufferSource, false, 0, 0xF000F0);

        // 弹药数
        font.drawInBatch(this.ammoCountText, pX + 20, pY + 13, 0x777777, false, matrix4f, bufferSource, false, 0, 0xF000F0);

        // 拥有者信息
        int yOffset = pY + 27;
        font.drawInBatch(this.ownerInfo, pX, yOffset, 0x777777, false, matrix4f, bufferSource, false, 0, 0xF000F0);
        yOffset += 11;

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
