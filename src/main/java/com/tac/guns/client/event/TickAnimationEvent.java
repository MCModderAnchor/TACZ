package com.tac.guns.client.event;

import com.tac.guns.GunMod;
import com.tac.guns.api.TimelessAPI;
import com.tac.guns.api.client.player.IClientPlayerGunOperator;
import com.tac.guns.api.item.IGun;
import com.tac.guns.client.animation.internal.GunAnimationStateMachine;
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
            if (animationStateMachine != null) {
                animationStateMachine.setAiming(clientGunOperator.getClientAimingProgress(1) > 0.5f);
                boolean isShooting = clientGunOperator.getClientShootCoolDown() > 0;
                // 如果玩家正在射击，只能处于 idle 状态
                if (isShooting) {
                    animationStateMachine
                            .onShooterIdle();
                }// 如果玩家正在移动，播放移动动画，否则播放 idle 动画
                else if (player.isSprinting()) {
                    animationStateMachine
                            .setOnGround(player.isOnGround())
                            .onShooterRun(player.walkDist);
                } else if (!player.isMovingSlowly() && player.input.getMoveVector().length() > 0.01) {
                    animationStateMachine
                            .setOnGround(player.isOnGround())
                            .onShooterWalk(player.input, player.walkDist);
                } else {
                    animationStateMachine.onShooterIdle();
                }
                int ammoCount = iGun.getCurrentAmmoCount(mainhandItem);
                if (ammoCount < 1) {
                    animationStateMachine.onGunCatchBolt();
                } else {
                    animationStateMachine.onGunReleaseBolt();
                }
                animationStateMachine.onIdleHoldingPose();
            }
        });
    }
}
