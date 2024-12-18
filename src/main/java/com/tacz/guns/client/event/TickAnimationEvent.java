package com.tacz.guns.client.event;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.client.animation.statemachine.GunAnimationConstant;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
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
        ItemStack mainhandItem = player.getMainHandItem();
        TimelessAPI.getGunDisplay(mainhandItem).ifPresent(gunIndex -> {
            var animationStateMachine = gunIndex.getAnimationStateMachine();
            // 群组服切世界导致的特殊 BUG 处理，正常情况不会遇到此问题
            if (player.input == null) {
                animationStateMachine.trigger(GunAnimationConstant.INPUT_IDLE);
                return;
            }
            if (!player.isMovingSlowly() && player.isSprinting()) {
                // 如果玩家正在移动，播放移动动画，否则播放 idle 动画
                animationStateMachine.trigger(GunAnimationConstant.INPUT_RUN);
            } else if (!player.isMovingSlowly() && player.input.getMoveVector().length() > 0.01) {
                animationStateMachine.trigger(GunAnimationConstant.INPUT_WALK);
            } else {
                animationStateMachine.trigger(GunAnimationConstant.INPUT_IDLE);
            }
        });
    }

    @SubscribeEvent
    public static void tickAnimation(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            return;
        }
        if (Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        ItemStack mainhandItem = player.getMainHandItem();
        TimelessAPI.getGunDisplay(mainhandItem).ifPresent(gunIndex -> {
            // 更新状态机
            var animationStateMachine = gunIndex.getAnimationStateMachine();
            animationStateMachine.processContextIfExist(context -> {
                context.setCurrentGunItem(mainhandItem);
                context.setPartialTicks(Minecraft.getInstance().getFrameTime());
            });
            animationStateMachine.visualUpdate();
        });
    }
}
