package com.tac.guns.event;

import com.tac.guns.api.item.IGun;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class PreventGunClick {
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        // 只要主手有枪，那么禁止交互
        ItemStack itemInHand = event.getPlayer().getItemInHand(InteractionHand.MAIN_HAND);
        if (itemInHand.getItem() instanceof IGun) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        // 只要主手有枪，那么禁止交互
        ItemStack itemInHand = event.getPlayer().getItemInHand(InteractionHand.MAIN_HAND);
        if (itemInHand.getItem() instanceof IGun) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        // 只要主手有枪，那么禁止交互
        ItemStack itemInHand = event.getPlayer().getItemInHand(InteractionHand.MAIN_HAND);
        if (itemInHand.getItem() instanceof IGun) {
            event.setCanceled(true);
        }
    }
}
