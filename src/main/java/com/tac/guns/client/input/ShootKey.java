package com.tac.guns.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.tac.guns.api.client.player.IClientPlayerGunOperator;
import com.tac.guns.api.gun.FireMode;
import com.tac.guns.api.item.IGun;
import com.tac.guns.client.sound.SoundPlayManager;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
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
    public static void autoShoot(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END && !isInGame()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        if (SHOOT_KEY.isDown()) {
            if (IGun.mainhandHoldGun(player) && IGun.getMainhandFireMode(player) == FireMode.AUTO) {
                IClientPlayerGunOperator.fromLocalPlayer(player).shoot();
            }
        }
    }

    @SubscribeEvent
    public static void semiShoot(InputEvent.MouseInputEvent event) {
        if (isInGame() && SHOOT_KEY.matchesMouse(event.getButton())) {
            // 松开鼠标，重置 DryFire 状态
            if (event.getAction() == GLFW.GLFW_RELEASE) {
                SoundPlayManager.resetDryFireSound();
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            if (player == null) {
                return;
            }
            if (IGun.mainhandHoldGun(player)) {
                FireMode fireMode = IGun.getMainhandFireMode(player);
                if (fireMode == FireMode.UNKNOWN) {
                    player.sendMessage(new TranslatableComponent("message.tac.fire_select.fail"), Util.NIL_UUID);
                }
                if (fireMode == FireMode.SEMI) {
                    IClientPlayerGunOperator.fromLocalPlayer(player).shoot();
                }
            }
        }
    }

    private static boolean isInGame() {
        Minecraft mc = Minecraft.getInstance();
        // 不能是加载界面
        if (mc.getOverlay() != null) {
            return false;
        }
        // 不能打开任何 GUI
        if (mc.screen != null) {
            return false;
        }
        // 当前窗口捕获鼠标操作
        if (!mc.mouseHandler.isMouseGrabbed()) {
            return false;
        }
        // 选择了当前窗口
        return mc.isWindowActive();
    }
}
