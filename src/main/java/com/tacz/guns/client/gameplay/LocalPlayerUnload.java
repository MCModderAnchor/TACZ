package com.tacz.guns.client.gameplay;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.client.animation.statemachine.GunAnimationConstant;
import com.tacz.guns.client.sound.SoundPlayManager;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ClientMessagePlayerUnloadGun;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class LocalPlayerUnload {
    private final LocalPlayerDataHolder data;
    private final LocalPlayer player;

    public LocalPlayerUnload(LocalPlayerDataHolder data, LocalPlayer player) {
        this.data = data;
        this.player = player;
    }

    public void unloadAmmo() {
        if (isLocalIdle()) {
            showCannotUnload();
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof AbstractGunItem gun)) {
            showCannotUnload();
            return;
        }
        IGunOperator operator = IGunOperator.fromLivingEntity(player);
        if (isSyncIdle(operator)) {
            showCannotUnload();
            return;
        }
        var gunId = gun.getGunId(stack);
        var index = TimelessAPI.getCommonGunIndex(gunId).orElse(null);
        if (index == null) {
            showCannotUnload();
            return;
        }
        if (!index.canUnloadAmmo() || gun.useInventoryAmmo(stack)) {
            player.displayClientMessage(Component.translatable("message.tacz.unload.unsupported"), true);
            return;
        }
        if (gun.getCurrentAmmoCount(stack) <= 0
                && (index.getGunData().getBolt() == Bolt.OPEN_BOLT || !gun.hasBulletInBarrel(stack))) {
            player.displayClientMessage(Component.translatable("message.tacz.unload.no_ammo"), true);
            return;
        }

        data.lockState();
        data.autoReloadSuppressed = true;
        data.chargeProgress = 0;
        TimelessAPI.getGunDisplay(stack).ifPresent(display -> {
            SoundPlayManager.stopPlayGunSound();
            var stateMachine = display.getAnimationStateMachine();
            if (stateMachine != null) {
                stateMachine.trigger(GunAnimationConstant.INPUT_INSPECT_BREAK_OUT);
            }
        });
        if (Minecraft.getInstance().gameMode != null) {
            Minecraft.getInstance().gameMode.ensureHasSentCarriedItem();
        }
        NetworkHandler.CHANNEL.sendToServer(new ClientMessagePlayerUnloadGun(player.getInventory().selected, gunId));
    }

    private void showCannotUnload() {
        player.displayClientMessage(Component.translatable("message.tacz.unload.cannot"), true);
    }

    private boolean isSyncIdle(IGunOperator operator) {
        return operator.getSynReloadState().getStateType().isReloading() || operator.getSynIsBolting()
                || operator.getSynShootCoolDown() != 0 || operator.getSynDrawCoolDown() != 0
                || operator.getSynMeleeCoolDown() != 0 || operator.getSynSprintTime() > 0
                || IClientPlayerGunOperator.fromLocalPlayer(player).getClientShootCoolDown() > 0;
    }

    private boolean isLocalIdle() {
        return !player.isAlive() || player.isSpectator() || data.clientStateLock
                || data.isCharging || data.isBolting || data.clientReloadStarting
                || !data.isShootRecorded || data.hasPendingShoot()
                || data.clientIsAiming || data.clientAimingProgress > 0 || player.isSprinting();
    }
}
