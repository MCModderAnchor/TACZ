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
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class InspectKey {
    public static final KeyMapping INSPECT_KEY = new KeyMapping("key.tac.inspect.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            "key.category.tac");

    @SubscribeEvent
    public static void onInspectPress(InputEvent.KeyInputEvent event) {
        if (event.getAction() == GLFW.GLFW_PRESS && INSPECT_KEY.matches(event.getKey(), event.getScanCode())) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }
            if (IGun.mainhandHoldGun(player)) {
                IClientPlayerGunOperator.fromLocalPlayer(player).inspect();
            }
        }
    }
}
