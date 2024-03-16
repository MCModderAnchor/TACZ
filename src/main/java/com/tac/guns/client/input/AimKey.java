package com.tac.guns.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.tac.guns.api.client.player.IClientPlayerGunOperator;
import com.tac.guns.api.item.IGun;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class AimKey {
    public static final KeyMapping AIM_KEY = new KeyMapping("key.tac.aim.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_RIGHT,
            "key.category.tac");

    @SubscribeEvent
    public static void onAimPress(InputEvent.MouseInputEvent event) {
        if (isInGame() && AIM_KEY.matchesMouse(event.getButton())) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }
            if (IGun.mainhandHoldGun(player)) {
                if (event.getAction() == GLFW.GLFW_PRESS) {
                    IClientPlayerGunOperator.fromLocalPlayer(player).aim(true);
                }
                if (event.getAction() == GLFW.GLFW_RELEASE) {
                    IClientPlayerGunOperator.fromLocalPlayer(player).aim(false);
                }
            }
        }
    }

    @SubscribeEvent
    public static void cancelAim(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (!(player instanceof IClientPlayerGunOperator operator)) {
            return;
        }
        if (operator.isAim() && !isInGame()) {
            IClientPlayerGunOperator.fromLocalPlayer(player).aim(false);
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
