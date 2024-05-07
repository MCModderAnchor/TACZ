package com.tacz.guns.client.event;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.player.IClientPlayerGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.animation.internal.GunAnimationStateMachine;
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
            animationStateMachine.setAiming(clientGunOperator.getClientAimingProgress(1) == 1f);
            boolean isShooting = clientGunOperator.getClientShootCoolDown() > 0;

            if (isShooting) {
                // 如果玩家正在射击，只能处于 idle 状态
                animationStateMachine.onShooterIdle();
            } else if (player.isSprinting()) {
                // 如果玩家正在移动，播放移动动画，否则播放 idle 动画
                animationStateMachine.setOnGround(player.isOnGround()).onShooterRun(player.walkDist);
            } else if (!player.isMovingSlowly() && player.input.getMoveVector().length() > 0.01) {
                animationStateMachine.setOnGround(player.isOnGround()).onShooterWalk(player.input, player.walkDist);
            } else {
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
