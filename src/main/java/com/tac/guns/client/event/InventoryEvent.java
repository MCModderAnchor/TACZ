package com.tac.guns.client.event;

import com.tac.guns.GunMod;
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
    private static ItemStack oldHotbarSelectedStack = ItemStack.EMPTY;
    private static int oldHotbarSelected = -1;

    @SubscribeEvent
    public static void onGunDraw(TickEvent.ClientTickEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        Inventory inventory = player.getInventory();
        ItemStack inventorySelected = inventory.getSelected();
        if (oldHotbarSelected != inventory.selected || !isSame(inventorySelected, oldHotbarSelectedStack)) {
            oldHotbarSelected = inventory.selected;
            oldHotbarSelectedStack = inventorySelected;
            IClientPlayerGunOperator.fromLocalPlayer(player).draw();
        }
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
