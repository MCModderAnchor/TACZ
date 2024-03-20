package com.tac.guns.client.event;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tac.guns.GunMod;
import com.tac.guns.api.TimelessAPI;
import com.tac.guns.api.client.player.IClientPlayerGunOperator;
import com.tac.guns.api.entity.IGunOperator;
import com.tac.guns.api.gun.ReloadState;
import com.tac.guns.api.item.IGun;
import com.tac.guns.client.animation.internal.GunAnimationStateMachine;
import com.tac.guns.client.gui.GunRefitScreen;
import com.tac.guns.client.renderer.crosshair.CrosshairType;
import com.tac.guns.config.client.RenderConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.gui.ForgeIngameGui;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static com.tac.guns.util.RenderHelper.blit;


@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = GunMod.MOD_ID)
public class GameOverlayEvent {
    private static final ResourceLocation HIT_ICON = new ResourceLocation(GunMod.MOD_ID, "textures/crosshair/hit/hit_marker.png");
    private static final long KEEP_TIME = 300;
    private static boolean isRefitScreen = false;
    private static long hitTimestamp = -1L;
    private static long killTimestamp = -1L;

    /**
     * 当玩家手上拿着枪时，播放特定动画、或瞄准时需要隐藏准心
     */
    @SubscribeEvent(receiveCanceled = true)
    public static void onRenderOverlay(RenderGameOverlayEvent.PreLayer event) {
        if (event.getOverlay() == ForgeIngameGui.CROSSHAIR_ELEMENT) {
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
            renderHitMarker(event.getMatrixStack(), event.getWindow());
            // 瞄准快要完成时，取消准心渲染
            if (IClientPlayerGunOperator.fromLocalPlayer(player).getClientAimingProgress(event.getPartialTicks()) > 0.9) {
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
                    renderCrosshair(event.getMatrixStack(), event.getWindow());
                }
            });
        }
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        // 奇迹的是，RenderGameOverlayEvent.PreLayer 事件中，screen还未被赋值...
        isRefitScreen = Minecraft.getInstance().screen instanceof GunRefitScreen;
    }

    private static void renderCrosshair(PoseStack poseStack, Window window) {
        int width = window.getGuiScaledWidth();
        int height = window.getGuiScaledHeight();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        ResourceLocation location = CrosshairType.getTextureLocation(RenderConfig.CROSSHAIR_TYPE.get());
        RenderSystem.setShaderTexture(0, location);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1F, 1F, 1F, 0.9f);
        float x = width / 2f - 8;
        float y = height / 2f - 8;
        blit(poseStack, x, y, 0, 0, 16, 16, 16, 16);
    }

    private static void renderHitMarker(PoseStack poseStack, Window window) {
        long remainHitTime = System.currentTimeMillis() - hitTimestamp;
        long remainKillTime = System.currentTimeMillis() - killTimestamp;
        float offset = 0;
        float fadeTime;

        if (remainKillTime > KEEP_TIME) {
            if (remainHitTime > KEEP_TIME) {
                return;
            } else {
                fadeTime = remainHitTime;
            }
        } else {
            // 最大位移为 4 像素
            offset = (remainKillTime * 4f) / KEEP_TIME;
            fadeTime = remainKillTime;
        }

        int width = window.getGuiScaledWidth();
        int height = window.getGuiScaledHeight();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, HIT_ICON);

        float x = width / 2f - 8;
        float y = height / 2f - 8;

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1 - fadeTime / KEEP_TIME);

        blit(poseStack, x - offset, y - offset, 0, 0, 8, 8, 16, 16);
        blit(poseStack, x + 8 + offset, y - offset, 8, 0, 8, 8, 16, 16);
        blit(poseStack, x - offset, y + 8 + offset, 0, 8, 8, 8, 16, 16);
        blit(poseStack, x + 8 + offset, y + 8 + offset, 8, 8, 8, 8, 16, 16);
    }

    public static void markHitTimestamp() {
        GameOverlayEvent.hitTimestamp = System.currentTimeMillis();
    }

    public static void markKillTimestamp() {
        GameOverlayEvent.killTimestamp = System.currentTimeMillis();
    }
}
