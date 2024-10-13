package com.tacz.guns.client.event;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.client.animation.statemachine.GunAnimationStateMachine;
import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = GunMod.MOD_ID)
public class TickAnimationEvent {
    @SubscribeEvent
    public static void tickAnimation(TickEvent.ClientTickEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        IClientPlayerGunOperator clientGunOperator = IClientPlayerGunOperator.fromLocalPlayer(player);
        ItemStack mainhandItem = player.getMainHandItem();
        if (!(mainhandItem.getItem() instanceof IGun iGun)) {
            return;
        }
        ResourceLocation gunId = iGun.getGunId(mainhandItem);
        TimelessAPI.getClientGunIndex(gunId).ifPresent(gunIndex -> {
            GunAnimationStateMachine animationStateMachine = gunIndex.getAnimationStateMachine();
            if (animationStateMachine == null) {
                return;
            }
            animationStateMachine
                .setAiming(clientGunOperator.getClientAimingProgress(1) == 1f)
                .setHolstering(false);
            boolean isShooting = clientGunOperator.getClientShootCoolDown() > 0;
            boolean isCorrectlyRunning = !player.isMovingSlowly() && player.isSprinting();

            boolean isHolsterAnimationAllowed = RenderConfig.ENABLE_HOLSTER_ANIMATION_ON_GUN_SAFETY.get();
            // Both aiming, inspecting and reloading actions are allowed in gun safety/player holster mode.
            // If the weapon is in safety, we need to cancel off the sprint animation for the animation of the allowed actions to play first.
            // This allows the animation of the ongoing action to play correctly without accounting for offsets, 
            // so it could blend back to holstering after the ongoing animation is done 
            boolean isPlayingAllowedActionsInSafety = animationStateMachine.isPlayingInspectAnimation() || 
                                                      animationStateMachine.isPlayingReloadAnimation() ||
                                                      clientGunOperator.isAim();
            boolean isWeaponInSafetyAndNotPlayingOtherActions = iGun.getFireMode(mainhandItem) == FireMode.SAFETY && !isPlayingAllowedActionsInSafety;
    
            if (isShooting) {
                // 如果玩家正在射击，只能处于静止状态
                animationStateMachine.onShooterIdle();
            } else if (isCorrectlyRunning) {
                // 如果玩家正在奔跑，播放奔跑动画
                animationStateMachine.setOnGround(player.onGround()).onShooterRun(player.walkDist);
            } else if (isHolsterAnimationAllowed && isWeaponInSafetyAndNotPlayingOtherActions) {
                // 如果玩家目前手持的枪正在保险模式，但正在特定的状态下，先继续播放特定动画直到完成，否则播放枪械保护动画
                animationStateMachine.setOnGround(player.onGround()).onShooterHolster(player.walkDist);
            } else if (!player.isMovingSlowly() && player.input.getMoveVector().length() > 0.01) {
                animationStateMachine.setOnGround(player.onGround()).onShooterWalk(player.input, player.walkDist);
            } else {
                // 缺省动画，静止状态
                animationStateMachine.onShooterIdle();
            }

            Bolt boltType = gunIndex.getGunData().getBolt();
            int ammoCount = iGun.getCurrentAmmoCount(mainhandItem) + (iGun.hasBulletInBarrel(mainhandItem) && boltType != Bolt.OPEN_BOLT ? 1 : 0);
            if (ammoCount < 1) {
                animationStateMachine.onGunCatchBolt();
            } else {
                animationStateMachine.onGunReleaseBolt();
            }
            animationStateMachine.onIdleHoldingPose();
        });
    }
}
