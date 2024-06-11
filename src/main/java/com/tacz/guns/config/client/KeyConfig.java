package com.tacz.guns.config.client;

import net.minecraftforge.common.ForgeConfigSpec;

public class KeyConfig {
    public static ForgeConfigSpec.BooleanValue HOLD_TO_AIM;

    public static void init(ForgeConfigSpec.Builder builder) {
        builder.push("key");

        builder.comment("True if you want to hold the right mouse button to aim");
        HOLD_TO_AIM = builder.define("HoldToAim", true);

        builder.pop();
    }
}
