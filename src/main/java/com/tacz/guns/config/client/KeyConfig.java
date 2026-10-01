package com.tacz.guns.config.client;

import net.neoforged.neoforge.common.ModConfigSpec;

public class KeyConfig {
    public static ModConfigSpec.BooleanValue HOLD_TO_AIM;
    public static ModConfigSpec.BooleanValue HOLD_TO_CRAWL;
    public static ModConfigSpec.BooleanValue AUTO_RELOAD;

    public static void init(ModConfigSpec.Builder builder) {
        builder.push("key");

        builder.comment("True if you want to hold the right mouse button to aim");
        HOLD_TO_AIM = builder.define("HoldToAim", true);

        builder.comment("True if you want to hold the crawl button to crawl");
        HOLD_TO_CRAWL = builder.define("HoldToCrawl", true);

        builder.comment("Try to reload automatically when the gun is empty");
        AUTO_RELOAD = builder.define("AutoReload", false);

        builder.pop();
    }
}
