package com.tacz.guns.client.gui.overlay;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.index.ClientGunIndex;
import com.tacz.guns.client.resource.pojo.display.gun.AmmoCountStyle;
import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.util.AttachmentDataUtils;
import com.tacz.guns.util.time.GunTime;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;

public class GunHudOverlay implements IGuiOverlay {
    private static final ResourceLocation SEMI = new ResourceLocation(GunMod.MOD_ID, "textures/hud/fire_mode_semi.png");
    private static final ResourceLocation AUTO = new ResourceLocation(GunMod.MOD_ID, "textures/hud/fire_mode_auto.png");
    private static final ResourceLocation BURST = new ResourceLocation(GunMod.MOD_ID, "textures/hud/fire_mode_burst.png");
    private static final ResourceLocation HEATBAR = new ResourceLocation(GunMod.MOD_ID, "textures/hud/heat_bar.png");
    private static final ResourceLocation HEATBASE = new ResourceLocation(GunMod.MOD_ID, "textures/hud/heat_base.png");

    private static final DecimalFormat CURRENT_AMMO_FORMAT = new DecimalFormat("000");
    private static final DecimalFormat CURRENT_AMMO_FORMAT_PERCENT = new DecimalFormat("000%");
    private static final DecimalFormat INVENTORY_AMMO_FORMAT = new DecimalFormat("0000");
    private static long checkAmmoTimestamp = -1L;
    private static int cacheMaxAmmoCount = 0;
    private static int cacheInventoryAmmoCount = 0;

    private static final int MAX_AMMO_COUNT = 9999;

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        if (!RenderConfig.GUN_HUD_ENABLE.get()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (!(player instanceof IClientPlayerGunOperator)) {
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

        // 是否背包直读
        boolean useInventoryAmmo = iGun.useInventoryAmmo(stack);
        // 是否使用虚拟备弹
        boolean useDummyAmmo = iGun.useDummyAmmo(stack);
        // 是否完全过热
        boolean overheatLocked = gunData.hasHeatData() && iGun.isOverheatLocked(stack);
        // 当前枪械弹药数
        int ammoCount = useInventoryAmmo ? cacheInventoryAmmoCount + (iGun.hasBulletInBarrel(stack) && gunData.getBolt() != Bolt.OPEN_BOLT ? 1 : 0) :
                iGun.getCurrentAmmoCount(stack) + (iGun.hasBulletInBarrel(stack) && gunData.getBolt() != Bolt.OPEN_BOLT ? 1 : 0);
        ammoCount = Math.min(ammoCount, MAX_AMMO_COUNT);
        // 弹药颜色
        int ammoCountColor;
        if (ammoCount < (cacheMaxAmmoCount * 0.25) && ammoCount < 10 || overheatLocked) {
            // 红色
            ammoCountColor = 0xFF5555;
        } else {
            // 如果背包直读并且使用虚拟备弹为青色，否则背包直读为黄色，其他为白色
            ammoCountColor = useInventoryAmmo && useDummyAmmo ? 0x55FFFF : useInventoryAmmo ? 0xFFFF55 : 0xFFFFFF;
        }
        // 备弹颜色
        int inventoryAmmoCountColor;
        if (!useInventoryAmmo && useDummyAmmo) {
            inventoryAmmoCountColor = 0x55FFFF;
        } else {
            inventoryAmmoCountColor = 0xAAAAAA;
        }

        // 当前枪械弹药数显示
        String currentAmmoCountText;
        if (display.getAmmoCountStyle() == AmmoCountStyle.PERCENT) {
            // 百分比模式
            currentAmmoCountText = CURRENT_AMMO_FORMAT_PERCENT.format((float)ammoCount/(cacheMaxAmmoCount==0 ? 1f : cacheMaxAmmoCount));
        } else {
            // 普通模式
            currentAmmoCountText = CURRENT_AMMO_FORMAT.format(ammoCount);
        }

        // 备弹数显示 (背包直读模式不显示备弹)
        String inventoryAmmoCountText = useInventoryAmmo ? "" : INVENTORY_AMMO_FORMAT.format(cacheInventoryAmmoCount);
        if (!useInventoryAmmo && gunData.getReloadData().isInfinite()) {
            inventoryAmmoCountText = "∞";
        }

        // 计算弹药数
        handleCacheCount(player, stack, gunData, iGun, useInventoryAmmo);

        // 竖线
        graphics.fill(width - 75, height - 43, width - 74, height - 25, 0xFFFFFFFF);

        PoseStack poseStack = graphics.pose();

        Font font = mc.font;

        // 数字
        poseStack.pushPose();
        poseStack.scale(1.5f, 1.5f, 1);
        graphics.drawString(font, currentAmmoCountText, (width - 70) / 1.5f, (height - 43) / 1.5f, ammoCountColor, false);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.scale(0.8f, 0.8f, 1);
        graphics.drawString(font, inventoryAmmoCountText, (width - 68 + mc.font.width(currentAmmoCountText) * 1.5f) / 0.8f, (height - 43) / 0.8f, inventoryAmmoCountColor, false);
        poseStack.popPose();

        // 模组版本信息
        String minecraftVersion = SharedConstants.getCurrentVersion().getName();
        String modVersion = ModList.get().getModFileById(GunMod.MOD_ID).versionString();
        String debugInfo = String.format("%s-%s", minecraftVersion, modVersion);
        // 文本
        poseStack.pushPose();
        poseStack.scale(0.5f, 0.5f, 1);
        graphics.drawString(font, debugInfo, (int) ((width - 70) / 0.5f), (int) ((height - 29f) / 0.5f), 0xffaaaaaa);
        poseStack.popPose();

        // 图标渲染
        RenderSystem.enableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // 获取图标
        ResourceLocation hudTexture = display.getHUDTexture();
        @Nullable ResourceLocation hudEmptyTexture = display.getHudEmptyTexture();

        if (ammoCount <= 0 || overheatLocked) {
            if (hudEmptyTexture == null) {
                RenderSystem.setShaderColor(1, 0.3f, 0.3f, 1);
            } else {
                hudTexture = hudEmptyTexture;
            }
        }
        // 渲染枪械图标
        graphics.blit(hudTexture, width - 117, height - 44, 0, 0, 39, 13, 39, 13);

        // 渲染开火模式图标
        FireMode fireMode = IGun.getMainHandFireMode(player);
        ResourceLocation fireModeTexture = switch (fireMode) {
            case AUTO -> AUTO;
            case BURST -> BURST;
            default -> SEMI;
        };
        RenderSystem.setShaderColor(1, 1, 1, 1);
        graphics.blit(fireModeTexture, (int) (width - 68.5 + mc.font.width(currentAmmoCountText) * 1.5), height - 38, 0, 0, 10, 10, 10, 10);
    }

    private static void handleCacheCount(LocalPlayer player, ItemStack stack, GunData gunData, IGun iGun, boolean useInventoryAmmo) {
        // 0.05 秒 (1 tick) 检查一次
        if (checkAmmoTimestamp < 0 || GunTime.hasElapsed(checkAmmoTimestamp, 50)) {
            checkAmmoTimestamp = GunTime.nowMillis();
            // 当前枪械的总弹药数
            cacheMaxAmmoCount = AttachmentDataUtils.getAmmoCountWithAttachment(stack, gunData);
            // 玩家背包弹药数
            if (IGunOperator.fromLivingEntity(player).needCheckAmmo()) {
                if (iGun.useDummyAmmo(stack)) {
                    // 缓存虚拟弹药数
                    cacheInventoryAmmoCount = iGun.getDummyAmmoAmount(stack);
                } else {
                    // 缓存背包内的弹药数
                    handleInventoryAmmo(stack, player.getInventory());
                }
            } else {
                cacheInventoryAmmoCount = MAX_AMMO_COUNT;
            }
            if (useInventoryAmmo) {
                iGun.setCurrentAmmoCount(stack, cacheInventoryAmmoCount);
            }
        }
    }

    public static void resetTimestamp() {
        checkAmmoTimestamp = -1L;
        cacheMaxAmmoCount = 0;
        cacheInventoryAmmoCount = 0;
    }

    private static void handleInventoryAmmo(ItemStack stack, Inventory inventory) {
        cacheInventoryAmmoCount = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack inventoryItem = inventory.getItem(i);
            if (inventoryItem.getItem() instanceof IAmmo iAmmo && iAmmo.isAmmoOfGun(stack, inventoryItem)) {
                cacheInventoryAmmoCount += inventoryItem.getCount();
            }
            if (inventoryItem.getItem() instanceof IAmmoBox iAmmoBox && iAmmoBox.isAmmoBoxOfGun(stack, inventoryItem)) {
                // 创造模式弹药箱？直接返回 9999
                if (iAmmoBox.isAllTypeCreative(inventoryItem) || iAmmoBox.isCreative(inventoryItem)) {
                    cacheInventoryAmmoCount = 9999;
                    return;
                }
                cacheInventoryAmmoCount += iAmmoBox.getAmmoCount(inventoryItem);
            }
        }
    }
}
