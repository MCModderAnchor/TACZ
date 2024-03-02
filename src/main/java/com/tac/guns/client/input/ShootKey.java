package com.tac.guns.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.tac.guns.api.entity.IShooter;
import com.tac.guns.api.event.GunShootEvent;
import com.tac.guns.api.item.IGun;
import com.tac.guns.client.animation.internal.GunAnimationStateMachine;
import com.tac.guns.client.model.BedrockGunModel;
import com.tac.guns.client.resource.ClientGunPackLoader;
import com.tac.guns.client.sound.SoundPlayManager;
import com.tac.guns.item.GunItem;
import com.tac.guns.network.NetworkHandler;
import com.tac.guns.network.message.ClientMessagePlayerShoot;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ShootKey {
    public static final KeyMapping SHOOT_KEY = new KeyMapping("key.tac.shoot.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_LEFT,
            "key.category.tac");

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END && !isInGame()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (SHOOT_KEY.isDown() && player instanceof IShooter shooter && IGun.mainhandHoldGun(player)) {
            ResourceLocation gunId = GunItem.getData(player.getMainHandItem()).getGunId();
            ClientGunPackLoader.getGunIndex(gunId).ifPresent(gunIndex -> {
                if ((System.currentTimeMillis() - shooter.getShootTime()) < gunIndex.getGunData().getShootInterval()) {
                    return;
                }
                if (MinecraftForge.EVENT_BUS.post(new GunShootEvent(player, player.getMainHandItem(), LogicalSide.CLIENT))) {
                    return;
                }
                BedrockGunModel gunModel = gunIndex.getGunModel();
                GunAnimationStateMachine animationStateMachine = gunIndex.getAnimationStateMachine();
                if (gunModel != null && animationStateMachine != null) {
                    animationStateMachine.onGunShoot();
                }
                NetworkHandler.CHANNEL.sendToServer(new ClientMessagePlayerShoot());
                SoundPlayManager.playClientSound(player, gunIndex.getSounds("shoot"), 1.0f, 0.8f);
                player.setXRot(player.getXRot() - 0.5f);
                shooter.recordShootTime();
            });
        }
    }

    private static boolean isInGame() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return false;
        }
        if (mc.getOverlay() != null) {
            return false;
        }
        if (mc.screen != null) {
            return false;
        }
        if (!mc.mouseHandler.isMouseGrabbed()) {
            return false;
        }
        return mc.isWindowActive();
    }
}
