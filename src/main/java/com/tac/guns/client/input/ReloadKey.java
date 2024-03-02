package com.tac.guns.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.tac.guns.api.item.IGun;
import com.tac.guns.client.animation.internal.GunAnimationStateMachine;
import com.tac.guns.client.model.BedrockGunModel;
import com.tac.guns.client.resource.ClientGunPackLoader;
import com.tac.guns.item.GunItem;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ReloadKey {
    public static final KeyMapping RELOAD_KEY = new KeyMapping("key.tac.reload.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.category.tac");

    @SubscribeEvent
    public static void onKeyboardInput(InputEvent.KeyInputEvent event) {
        if (RELOAD_KEY.isDown()) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null && IGun.mainhandHoldGun(player)) {
                ResourceLocation gunId = GunItem.getData(player.getMainHandItem()).getGunId();
                ClientGunPackLoader.getGunIndex(gunId).ifPresent(gunIndex -> {
                    BedrockGunModel gunModel = gunIndex.getGunModel();
                    GunAnimationStateMachine animationStateMachine = gunIndex.getAnimationStateMachine();
                    if (gunModel != null && animationStateMachine != null) {
                        animationStateMachine.setNoAmmo(true).onGunReload();
                    }
                });
            }
        }
    }
}
