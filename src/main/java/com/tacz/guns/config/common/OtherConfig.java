package com.tacz.guns.config.common;

import net.neoforged.neoforge.common.ModConfigSpec;

public class OtherConfig {
    public static ModConfigSpec.BooleanValue DEFAULT_PACK_DEBUG;
    public static ModConfigSpec.IntValue TARGET_SOUND_DISTANCE;
    public static ModConfigSpec.DoubleValue SERVER_HITBOX_OFFSET;
    public static ModConfigSpec.BooleanValue SERVER_HITBOX_LATENCY_FIX;
    public static ModConfigSpec.DoubleValue SERVER_HITBOX_LATENCY_MAX_SAVE_MS;

    public static void init(ModConfigSpec.Builder builder) {
        builder.push("other");

        builder.comment("Deprecated and ignored: use [gunpack].DefaultPackDebug in <game directory>/tacz/tacz-pre.toml.");
        DEFAULT_PACK_DEBUG = builder.define("DefaultPackDebug", false);

        builder.comment("The farthest sound distance of the target, including minecarts type");
        TARGET_SOUND_DISTANCE = builder.defineInRange("TargetSoundDistance", 128, 0, Integer.MAX_VALUE);

        serverConfig(builder);

        builder.pop();
    }

    /**
     * 这些配置不加入 cloth config api 中
     */
    private static void serverConfig(ModConfigSpec.Builder builder) {
        builder.comment("DEV: Server hitbox offset (If the hitbox is ahead, fill in a negative number)");
        SERVER_HITBOX_OFFSET = builder.defineInRange("ServerHitboxOffset", 3, -Double.MAX_VALUE, Double.MAX_VALUE);

        builder.comment("Server hitbox latency fix");
        SERVER_HITBOX_LATENCY_FIX = builder.define("ServerHitboxLatencyFix", true);

        builder.comment("The maximum latency (in milliseconds) for the server hitbox latency fix saved");
        SERVER_HITBOX_LATENCY_MAX_SAVE_MS = builder.defineInRange("ServerHitboxLatencyMaxSaveMs", 1000, 250, Double.MAX_VALUE);
    }
}
