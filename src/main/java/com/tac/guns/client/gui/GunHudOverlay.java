package com.tac.guns.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tac.guns.GunMod;
import com.tac.guns.api.TimelessAPI;
import com.tac.guns.api.client.player.IClientPlayerGunOperator;
import com.tac.guns.api.entity.IGunOperator;
import com.tac.guns.api.gun.FireMode;
import com.tac.guns.api.item.IAmmo;
import com.tac.guns.api.item.IAmmoBox;
import com.tac.guns.api.item.IGun;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.ForgeIngameGui;

import java.text.DecimalFormat;

public class GunHudOverlay {
    private static final ResourceLocation SEMI = new ResourceLocation(GunMod.MOD_ID, "textures/hud/fire_mode_semi.png");
    private static final ResourceLocation AUTO = new ResourceLocation(GunMod.MOD_ID, "textures/hud/fire_mode_auto.png");
    private static final ResourceLocation BURST = new ResourceLocation(GunMod.MOD_ID, "textures/hud/fire_mode_burst.png");
    private static final DecimalFormat CURRENT_AMMO_FORMAT = new DecimalFormat("000");
    private static final DecimalFormat INVENTORY_AMMO_FORMAT = new DecimalFormat("0000");
    private static long checkAmmoTimestamp = -1L;
    private static int cacheInventoryAmmoCount = 0;

    public static void render(ForgeIngameGui gui, PoseStack poseStack, float partialTick, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (!(player instanceof IClientPlayerGunOperator clientPlayerGunOperator)) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof IGun iGun)) {
            return;
        }

        // 当前枪械弹药数
        int currentAmmoCount = iGun.getCurrentAmmoCount(stack);
        int ammoCountColor;
        if (currentAmmoCount < 1) {
            ammoCountColor = 0xFF5555;
        } else {
            ammoCountColor = 0xFFFFFF;
        }
        String currentAmmoCountText = CURRENT_AMMO_FORMAT.format(currentAmmoCount);


        // 玩家背包弹药数
        if (IGunOperator.fromLivingEntity(player).needCheckAmmo()) {
            // 0.2 秒检查一次
            if ((System.currentTimeMillis() - checkAmmoTimestamp) > 200) {
                Inventory inventory = player.getInventory();
                cacheInventoryAmmoCount = 0;
                for (int i = 0; i < inventory.getContainerSize(); i++) {
                    ItemStack inventoryItem = inventory.getItem(i);
                    if (inventoryItem.getItem() instanceof IAmmo iAmmo && iAmmo.isAmmoOfGun(stack, inventoryItem)) {
                        cacheInventoryAmmoCount += inventoryItem.getCount();
                    }
                    if (inventoryItem.getItem() instanceof IAmmoBox iAmmoBox && iAmmoBox.isAmmoBoxOfGun(stack, inventoryItem)) {
                        cacheInventoryAmmoCount += iAmmoBox.getAmmoCount(inventoryItem);
                    }
                }
                checkAmmoTimestamp = System.currentTimeMillis();
            }
        } else {
            cacheInventoryAmmoCount = 9999;
        }
        String inventoryAmmoCountText = INVENTORY_AMMO_FORMAT.format(cacheInventoryAmmoCount);


        TimelessAPI.getClientGunIndex(iGun.getGunId(stack)).ifPresent(gunIndex -> {
            // 竖线
            GuiComponent.fill(poseStack, width - 75, height - 43, width - 74, height - 32, 0xFFFFFFFF);

            // 数字
            poseStack.pushPose();
            poseStack.scale(1.5f, 1.5f, 1);
            mc.font.drawShadow(poseStack, currentAmmoCountText, (width - 70) / 1.5f, (height - 43) / 1.5f, ammoCountColor);
            poseStack.popPose();

            poseStack.pushPose();
            poseStack.scale(0.8f, 0.8f, 1);
            mc.font.drawShadow(poseStack, inventoryAmmoCountText, (width - 41) / 0.8f, (height - 43) / 0.8f, 0xAAAAAA);
            poseStack.popPose();

            // 图标渲染
            RenderSystem.enableDepthTest();
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            // 渲染枪械图标
            RenderSystem.setShaderTexture(0, gunIndex.getHUDTexture());
            GuiComponent.blit(poseStack, width - 142, height - 96, 0, 0, 64, 64, 64, 64);

            // 渲染开火模式图标
            FireMode fireMode = IGun.getMainhandFireMode(player);
            switch (fireMode) {
                case SEMI -> RenderSystem.setShaderTexture(0, SEMI);
                case AUTO -> RenderSystem.setShaderTexture(0, AUTO);
                case BURST -> RenderSystem.setShaderTexture(0, BURST);
            }
            GuiComponent.blit(poseStack, width - 41, height - 38, 0, 0, 10, 10, 10, 10);
        });
    }
}
