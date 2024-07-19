package com.tacz.guns.compat.controllable;

import net.minecraftforge.fml.ModList;

public class ControllableCompat {
    private static final String MOD_ID = "controllable";

    public static void init() {
        if (ModList.get().isLoaded(MOD_ID)) {
            ControllableInner.init();
        }
    }
}
