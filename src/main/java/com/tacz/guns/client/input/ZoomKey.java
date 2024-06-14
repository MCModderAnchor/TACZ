package com.tacz.guns.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ClientMessagePlayerZoom;
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

import static com.tacz.guns.util.InputExtraCheck.isInGame;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ZoomKey {
    public static final KeyMapping ZOOM_KEY = new KeyMapping("key.tacz.zoom.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "key.category.tacz");

    @SubscribeEvent
    public static void onReloadPress(InputEvent.Key event) {
        if (isInGame() && event.getAction() == GLFW.GLFW_PRESS && ZOOM_KEY.matches(event.getKey(), event.getScanCode())) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null || player.isSpectator()) {
                return;
            }
            IClientPlayerGunOperator operator = IClientPlayerGunOperator.fromLocalPlayer(player);
            if (operator.isAim()) {
                NetworkHandler.CHANNEL.sendToServer(new ClientMessagePlayerZoom());
            } else {
                operator.melee();
            }
        }
    }
}
