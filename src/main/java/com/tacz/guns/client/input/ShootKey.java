package com.tacz.guns.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.client.gameplay.LocalPlayerSprint;
import com.tacz.guns.client.sound.SoundPlayManager;
import com.tacz.guns.compat.controllable.ControllableCompat;
import com.tacz.guns.entity.shooter.LivingEntityShoot;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ClientMessagePlayerAutoShoot;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import static com.tacz.guns.util.InputExtraCheck.isInGame;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ShootKey {
    public static final KeyMapping SHOOT_KEY = new KeyMapping("key.tacz.shoot.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_LEFT,
            "key.category.tacz");
    private static boolean lastTimeShootSuccess = false;
    private static boolean controllerShootDown = false;
    private static boolean autoShootSent = false;

    @SubscribeEvent
    public static void autoShoot(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !isInGame()) {
            return;
        }
        LocalPlayerSprint.stopSprint = false;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || player.isSpectator()) {
            return;
        }
        ItemStack mainHandItem = player.getMainHandItem();
        if (mainHandItem.getItem() instanceof IGun iGun) {
            FireMode fireMode = iGun.getFireMode(mainHandItem);
            boolean isBurstAuto = fireMode == FireMode.BURST && TimelessAPI.getCommonGunIndex(iGun.getGunId(mainHandItem))
                    .map(index -> index.getGunData().getBurstData().isContinuousShoot())
                    .orElse(false);
            boolean isAutoMode = LivingEntityShoot.isAutoShootMode(fireMode, iGun, mainHandItem);
            IClientPlayerGunOperator operator = IClientPlayerGunOperator.fromLocalPlayer(player);
            boolean isShootDown = SHOOT_KEY.isDown() || controllerShootDown;

            if (isAutoMode) {
                if (operator.chargeShoot(isShootDown)) {
                    LocalPlayerSprint.stopSprint = true;
                    if (operator.shoot() == ShootResult.SUCCESS) {
                        if (!autoShootSent) {
                            NetworkHandler.CHANNEL.sendToServer(new ClientMessagePlayerAutoShoot(true));
                            autoShootSent = true;
                        }
                        lastTimeShootSuccess = true;
                        ControllableCompat.onGunShoot(mainHandItem, fireMode);
                    }
                }
                if (isShootDown) {
                    LocalPlayerSprint.stopSprint = true;
                } else {
                    if (autoShootSent) {
                        NetworkHandler.CHANNEL.sendToServer(new ClientMessagePlayerAutoShoot(false));
                        autoShootSent = false;
                    }
                    lastTimeShootSuccess = false;
                    SoundPlayManager.resetDryFireSound();
                }
            } else {
                if (autoShootSent) {
                    NetworkHandler.CHANNEL.sendToServer(new ClientMessagePlayerAutoShoot(false));
                    autoShootSent = false;
                }
                if (operator.chargeShoot(isShootDown)) {
                    LocalPlayerSprint.stopSprint = true;
                    if (fireMode != FireMode.AUTO && !isBurstAuto && lastTimeShootSuccess) {
                        return;
                    }
                    if (operator.shoot() == ShootResult.SUCCESS) {
                        lastTimeShootSuccess = true;
                        ControllableCompat.onGunShoot(mainHandItem, fireMode);
                    }
                }
                if (isShootDown) {
                    LocalPlayerSprint.stopSprint = true;
                } else {
                    lastTimeShootSuccess = false;
                    SoundPlayManager.resetDryFireSound();
                }
            }
        } else {
            if (autoShootSent) {
                NetworkHandler.CHANNEL.sendToServer(new ClientMessagePlayerAutoShoot(false));
                autoShootSent = false;
            }
        }
    }

    public static boolean shootControllerTick(boolean isShootDown) {
        controllerShootDown = isShootDown;
        return false;
    }

}
