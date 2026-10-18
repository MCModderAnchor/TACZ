package com.tacz.guns.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

import static com.tacz.guns.util.InputExtraCheck.isInGame;

@OnlyIn(Dist.CLIENT)
public class UnloadKey {
    public static final KeyMapping UNLOAD_KEY = new KeyMapping("key.tacz.unload.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.ALT,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.category.tacz");

    public static void onUnloadPress() {
        if (!isInGame()) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.isAlive() && !player.isSpectator()) {
            IClientPlayerGunOperator.fromLocalPlayer(player).unloadAmmo();
        }
    }
}
