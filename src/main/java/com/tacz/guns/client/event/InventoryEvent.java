package com.tacz.guns.client.event;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.client.event.SwapItemWithOffHand;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.item.IGun;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = GunMod.MOD_ID)
public class InventoryEvent {
    // 用于切枪逻辑
    private static int oldHotbarSelected = -1;
    private static ItemStack oldHotbarSelectItem = ItemStack.EMPTY;

    @SubscribeEvent
    public static void onPlayerChangeSelect(TickEvent.ClientTickEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        Inventory inventory = player.getInventory();
        // 玩家切换选中框的情况
        if (oldHotbarSelected != inventory.selected) {
            if (oldHotbarSelected == -1) {
                IClientPlayerGunOperator.fromLocalPlayer(player).draw(ItemStack.EMPTY);
            } else {
                IClientPlayerGunOperator.fromLocalPlayer(player).draw(inventory.getItem(oldHotbarSelected));
            }
            oldHotbarSelected = inventory.selected;
            oldHotbarSelectItem = inventory.getItem(inventory.selected).copy();
            return;
        }
        // 玩家选中的物品改变的情况
        ItemStack currentItem = inventory.getItem(inventory.selected);
        if (!ItemStack.matches(oldHotbarSelectItem, currentItem)) {
            if (!isSame(oldHotbarSelectItem, currentItem)) {
                IClientPlayerGunOperator.fromLocalPlayer(player).draw(oldHotbarSelectItem);
            }
            oldHotbarSelectItem = currentItem.copy();
        }
    }

    @SubscribeEvent
    public static void onPlayerSwapMainHand(SwapItemWithOffHand event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        IClientPlayerGunOperator.fromLocalPlayer(player).draw(player.getMainHandItem());
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        // 离开游戏时重置客户端 draw 状态
        oldHotbarSelected = -1;
        oldHotbarSelectItem = ItemStack.EMPTY;
    }

    private static boolean isSame(ItemStack i, ItemStack j) {
        IGun iGun1 = IGun.getIGunOrNull(i);
        IGun iGun2 = IGun.getIGunOrNull(j);
        if (iGun1 != null && iGun2 != null) {
            return iGun1.getGunId(i).equals(iGun2.getGunId(j));
        }
        if (i.isEmpty() || j.isEmpty()) {
            return i.isEmpty() && j.isEmpty();
        }
        return ItemStack.matches(i, j);
    }
}
