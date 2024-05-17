package com.tacz.guns.client.event;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ReloadState;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.animation.internal.GunAnimationStateMachine;
import com.tacz.guns.client.gui.GunRefitScreen;
import com.tacz.guns.client.renderer.crosshair.CrosshairType;
import com.tacz.guns.config.client.RenderConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static com.tacz.guns.util.RenderHelper.blit;


@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = GunMod.MOD_ID)
public class RenderCrosshairEvent {
    private static final ResourceLocation HIT_ICON = new ResourceLocation(GunMod.MOD_ID, "textures/crosshair/hit/hit_marker.png");
    private static final long KEEP_TIME = 300;
    private static boolean isRefitScreen = false;
    private static long hitTimestamp = -1L;
    private static long killTimestamp = -1L;
    private static long headShotTimestamp = -1L;

    /**
     * 当玩家手上拿着枪时，播放特定动画、或瞄准时需要隐藏准心
     */
    @SubscribeEvent(receiveCanceled = true)
    public static void onRenderOverlay(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay().id().equals(VanillaGuiOverlay.CROSSHAIR.id())) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }
            if (!IGun.mainhandHoldGun(player)) {
                return;
            }
            // 全面替换成自己的
            event.setCanceled(true);
            // 击中显示
            renderHitMarker(event.getGuiGraphics(), event.getWindow());
            // 瞄准快要完成时，取消准心渲染
            if (IClientPlayerGunOperator.fromLocalPlayer(player).getClientAimingProgress(event.getPartialTick()) > 0.9) {
                return;
            }
            // 换弹进行时取消准心渲染
            ReloadState reloadState = IGunOperator.fromLivingEntity(player).getSynReloadState();
            if (reloadState.getStateType().isReloading()) {
                return;
            }
            // 打开枪械改装界面的时候，取消准心渲染
            if (isRefitScreen) {
                return;
            }
            // 播放的动画需要隐藏准心时，取消准心渲染
            ItemStack stack = player.getMainHandItem();
            if (!(stack.getItem() instanceof IGun iGun)) {
                return;
            }
            ResourceLocation gunId = iGun.getGunId(stack);
            TimelessAPI.getClientGunIndex(gunId).ifPresent(gunIndex -> {
                GunAnimationStateMachine animationStateMachine = gunIndex.getAnimationStateMachine();
                if (!animationStateMachine.shouldHideCrossHair()) {
                    renderCrosshair(event.getGuiGraphics(), event.getWindow());
                }
            });
        }
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        // 奇迹的是，RenderGameOverlayEvent.PreLayer 事件中，screen 还未被赋值...
        isRefitScreen = Minecraft.getInstance().screen instanceof GunRefitScreen;
    }

    private static void renderCrosshair(GuiGraphics graphics, Window window) {
        Options options = Minecraft.getInstance().options;
        if (!options.getCameraType().isFirstPerson()) {
            return;
        }
        if (options.hideGui) {
            return;
        }
        MultiPlayerGameMode gameMode = Minecraft.getInstance().gameMode;
        if (gameMode == null) {
            return;
        }
        if (gameMode.getPlayerMode() == GameType.SPECTATOR) {
            return;
        }
        int width = window.getGuiScaledWidth();
        int height = window.getGuiScaledHeight();

        ResourceLocation location = CrosshairType.getTextureLocation(RenderConfig.CROSSHAIR_TYPE.get());

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1F, 1F, 1F, 0.9f);
        float x = width / 2f - 8;
        float y = height / 2f - 8;
        graphics.blit(location, (int) x, (int) y, 0, 0, 16, 16, 16, 16);
    }

    private static void renderHitMarker(GuiGraphics graphics, Window window) {
        long remainHitTime = System.currentTimeMillis() - hitTimestamp;
        long remainKillTime = System.currentTimeMillis() - killTimestamp;
        long remainHeadShotTime = System.currentTimeMillis() - headShotTimestamp;
        float offset = RenderConfig.HIT_MARKET_START_POSITION.get().floatValue();
        float fadeTime;

        if (remainKillTime > KEEP_TIME) {
            if (remainHitTime > KEEP_TIME) {
                return;
            } else {
                fadeTime = remainHitTime;
            }
        } else {
            // 最大位移为 4 像素
            offset += (remainKillTime * 4f) / KEEP_TIME;
            fadeTime = remainKillTime;
        }

        int width = window.getGuiScaledWidth();
        int height = window.getGuiScaledHeight();
        float x = width / 2f - 8;
        float y = height / 2f - 8;

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        if (remainHeadShotTime > KEEP_TIME) {
            RenderSystem.setShaderColor(1F, 1F, 1F, 1 - fadeTime / KEEP_TIME);
        } else {
            RenderSystem.setShaderColor(1F, 0, 0, 1 - fadeTime / KEEP_TIME);
        }

        graphics.blit(HIT_ICON, (int) (x - offset), (int) (y - offset), 0, 0, 8, 8, 16, 16);
        graphics.blit(HIT_ICON, (int) (x + 8 + offset), (int) (y - offset), 8, 0, 8, 8, 16, 16);
        graphics.blit(HIT_ICON, (int) (x - offset), (int) (y + 8 + offset), 0, 8, 8, 8, 16, 16);
        graphics.blit(HIT_ICON, (int) (x + 8 + offset), (int) (y + 8 + offset), 8, 8, 8, 8, 16, 16);
    }

    public static void markHitTimestamp() {
        RenderCrosshairEvent.hitTimestamp = System.currentTimeMillis();
    }

    public static void markKillTimestamp() {
        RenderCrosshairEvent.killTimestamp = System.currentTimeMillis();
    }

    public static void markHeadShotTimestamp() {
        RenderCrosshairEvent.headShotTimestamp = System.currentTimeMillis();
    }
}
