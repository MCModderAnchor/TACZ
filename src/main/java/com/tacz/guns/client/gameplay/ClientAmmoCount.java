package com.tacz.guns.client.gameplay;

import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.api.item.IGun;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Reads inventory-fed ammo without changing the gun's ItemStack. */
public final class ClientAmmoCount {
    public static final int MAX_DISPLAY_COUNT = 9999;

    private ClientAmmoCount() {
    }

    /** Display consumers share one short-lived count; gameplay calls {@link #get} directly. */
    public static int getCached(LocalPlayer player, ItemStack gun, IGun iGun) {
        return IClientPlayerGunOperator.fromLocalPlayer(player).getDataHolder().getCachedAmmoCount(gun, iGun);
    }

    public static int get(LocalPlayer player, ItemStack gun, IGun iGun) {
        if (!IGunOperator.fromLivingEntity(player).needCheckAmmo()) {
            return MAX_DISPLAY_COUNT;
        }
        if (iGun.useDummyAmmo(gun)) {
            return Math.max(0, Math.min(iGun.getDummyAmmoAmount(gun), MAX_DISPLAY_COUNT));
        }

        int count = 0;
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.getItem() instanceof IAmmo ammo && ammo.isAmmoOfGun(gun, stack)) {
                count += Math.min(stack.getCount(), MAX_DISPLAY_COUNT - count);
            }
            if (stack.getItem() instanceof IAmmoBox box && box.isAmmoBoxOfGun(gun, stack)) {
                if (box.isAllTypeCreative(stack) || box.isCreative(stack)) {
                    return MAX_DISPLAY_COUNT;
                }
                count += Math.min(Math.max(box.getAmmoCount(stack), 0), MAX_DISPLAY_COUNT - count);
            }
            if (count >= MAX_DISPLAY_COUNT) {
                return MAX_DISPLAY_COUNT;
            }
        }
        return count;
    }
}
