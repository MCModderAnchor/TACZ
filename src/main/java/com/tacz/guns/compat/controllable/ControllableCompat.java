package com.tacz.guns.compat.controllable;

import com.tacz.guns.api.item.gun.FireMode;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

public class ControllableCompat {
    private static final String MOD_ID = "controllable";

    public static void init() {
        if (ModList.get().isLoaded(MOD_ID)) {
            ControllableInner.init();
        }
    }

    public static void onGunShoot(ItemStack gunItem, FireMode fireMode) {
        if (ModList.get().isLoaded(MOD_ID)) {
            ControllableInner.rumbleShoot(gunItem, fireMode);
        }
    }
}
