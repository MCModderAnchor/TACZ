package com.tac.guns.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.tac.guns.init.ModItems;
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
public class CheckGunKey {
    public static final KeyMapping CHECK_GUN_KEY = new KeyMapping("key.tac.check_gun.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            "key.category.tac");

    @SubscribeEvent
    public static void onKeyboardInput(InputEvent.KeyInputEvent event) {
        if (CHECK_GUN_KEY.isDown()) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null && player.getMainHandItem().is(ModItems.GUN.get())) {

            }
        }
    }
}
