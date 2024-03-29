package com.tac.guns.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.tac.guns.compat.cloth.MenuIntegration;
import com.tac.guns.init.CompatRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import static com.tac.guns.util.InputExtraCheck.isInGame;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ConfigKey {
    public static final KeyMapping OPEN_CONFIG_KEY = new KeyMapping("key.tac.open_config.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.ALT,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_T,
            "key.category.tac");

    @SubscribeEvent
    public static void onOpenConfig(InputEvent.KeyInputEvent event) {
        if (isInGame() && event.getAction() == GLFW.GLFW_PRESS
                && OPEN_CONFIG_KEY.matches(event.getKey(), event.getScanCode())
                && OPEN_CONFIG_KEY.getKeyModifier().equals(KeyModifier.getActiveModifier())) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }
            CompatRegistry.checkModLoad(CompatRegistry.CLOTH_CONFIG, () -> Minecraft.getInstance().setScreen(MenuIntegration.getConfigScreen(null)));
        }
    }
}
