package com.tacz.guns.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.config.client.KeyConfig;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import static com.tacz.guns.util.InputExtraCheck.isInGame;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class AimKey {
    public static final KeyMapping AIM_KEY = new KeyMapping("key.tacz.aim.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_RIGHT,
            "key.category.tacz");

    @SubscribeEvent
    public static void onAimPress(InputEvent.MouseButton.Post event) {
        if (isInGame() && AIM_KEY.matchesMouse(event.getButton())) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null || player.isSpectator()) {
                return;
            }
            if (!(player instanceof IClientPlayerGunOperator operator)) {
                return;
            }
            if (IGun.mainHandHoldGun(player)) {
                boolean action = true;
                if (!KeyConfig.HOLD_TO_AIM.get()) {
                    action = !operator.isAim();
                }
                if (event.getAction() == GLFW.GLFW_PRESS) {
                    IClientPlayerGunOperator.fromLocalPlayer(player).aim(action);
                }
                if (KeyConfig.HOLD_TO_AIM.get() && event.getAction() == GLFW.GLFW_RELEASE) {
                    IClientPlayerGunOperator.fromLocalPlayer(player).aim(false);
                }
            }
        }
    }

    /**
     * 该监听器能正确处理 按住瞄准模式 下的
     * 1.预输入（典型：按住瞄准切换武器后，能保持瞄准状态）
     * 2.键盘按键输入
     * 
     * 建议将按下切换瞄准也支持 键盘按键输入
     * */
    @SubscribeEvent
    public static void onAimHoldingPreInput(ClientTickEvent event) {
        if (!KeyConfig.HOLD_TO_AIM.get()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        boolean press = GLFW.glfwGetMouseButton(mc.getWindow().getWindow(), AimKey.AIM_KEY.getKey().getValue()) == GLFW.GLFW_PRESS;
        press = press || GLFW.glfwGetKey(mc.getWindow().getWindow(), AimKey.AIM_KEY.getKey().getValue()) == GLFW.GLFW_PRESS;
        if (InputExtraCheck.isInGame()) {
            LocalPlayer player = mc.player;
            if (player == null || player.isSpectator()) {
                return;
            }
            if (!(player instanceof IClientPlayerGunOperator operator)) {
                return;
            }
            if (operator.isAim() && press) {
                return;
            }
            if (!operator.isAim()) {
                if (!press) {
                    return;
                }
            }
            if (IGun.mainHandHoldGun(player)) {
                IClientPlayerGunOperator.fromLocalPlayer(player).aim(press);
            }
        }
    }

    public static boolean onAimControllerPress(boolean isPress) {
        if (!isInGame()) {
            return false;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.isSpectator()) {
            return false;
        }
        if (!(player instanceof IClientPlayerGunOperator operator)) {
            return false;
        }
        if (!IGun.mainHandHoldGun(player)) {
            return false;
        }
        boolean action = true;
        if (!KeyConfig.HOLD_TO_AIM.get()) {
            action = !operator.isAim();
        }
        if (isPress) {
            IClientPlayerGunOperator.fromLocalPlayer(player).aim(action);
            return true;
        }
        if (KeyConfig.HOLD_TO_AIM.get()) {
            IClientPlayerGunOperator.fromLocalPlayer(player).aim(false);
            return true;
        }
        return false;
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
        if (operator.isAim() && (!isInGame() || player.isSpectator())) {
            IClientPlayerGunOperator.fromLocalPlayer(player).aim(false);
        }
    }
}
