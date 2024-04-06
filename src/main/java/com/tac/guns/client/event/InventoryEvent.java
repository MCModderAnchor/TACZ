package com.tac.guns.client.event;

import com.tac.guns.GunMod;
import com.tac.guns.api.client.event.SwapItemWithOffHand;
import com.tac.guns.api.client.player.IClientPlayerGunOperator;
import com.tac.guns.api.item.IGun;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = GunMod.MOD_ID)
public class InventoryEvent {
    // 用于切枪逻辑
    private static int oldHotbarSelected = -1;

    @SubscribeEvent
    public static void onPlayerChangeSelect(TickEvent.ClientTickEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        Inventory inventory = player.getInventory();
        if (oldHotbarSelected != inventory.selected) {
            IClientPlayerGunOperator.fromLocalPlayer(player).draw(oldHotbarSelected, inventory.selected);
            oldHotbarSelected = inventory.selected;
        }
    }

    @SubscribeEvent
    public static void onPlayerSwapMainHand(SwapItemWithOffHand event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        Inventory inventory = player.getInventory();
        int offhandIndex = inventory.items.size() + inventory.armor.size() + inventory.offhand.size() - 1;
        IClientPlayerGunOperator.fromLocalPlayer(player).draw(offhandIndex, player.getInventory().selected);
    }

    /**
     * 判断两个枪械 ID 是否相同
     */
    private static boolean isSame(ItemStack gunA, ItemStack gunB) {
        if (gunA.getItem() instanceof IGun iGunA && gunB.getItem() instanceof IGun iGunB) {
            return iGunA.getGunId(gunA).equals(iGunB.getGunId(gunB));
        }
        return gunA.sameItem(gunB);
    }
}
