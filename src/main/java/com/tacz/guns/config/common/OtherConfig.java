package com.tacz.guns.config.common;

import net.minecraftforge.common.ForgeConfigSpec;

public class OtherConfig {
    public static ForgeConfigSpec.BooleanValue DEFAULT_PACK_DEBUG;
    public static ForgeConfigSpec.IntValue TARGET_SOUND_DISTANCE;
    public static ForgeConfigSpec.DoubleValue SERVER_HITBOX_OFFSET;
    public static ForgeConfigSpec.BooleanValue SERVER_HITBOX_LATENCY_FIX;
    public static ForgeConfigSpec.DoubleValue SERVER_HITBOX_LATENCY_MAX_SAVE_MS;
    public static ForgeConfigSpec.DoubleValue BULLET_REFLECTION_FACTOR;
    public static ForgeConfigSpec.DoubleValue BULLET_WHIZZ;

    public static void init(ForgeConfigSpec.Builder builder) {
        builder.push("other");

        builder.comment("Deprecated: now move to .minecraft/tacz/tacz-pre.toml or <your version>/tacz/tacz-pre.toml");
        builder.comment("When enabled, the reload command will not overwrite the default model file under config");
        DEFAULT_PACK_DEBUG = builder.define("DefaultPackDebug", false);

        builder.comment("The farthest sound distance of the target, including minecarts type");
        TARGET_SOUND_DISTANCE = builder.defineInRange("TargetSoundDistance", 128, 0, Integer.MAX_VALUE);

        builder.comment("This value is a scale factor for the distance from which players will hear or not hear a reflected sound.");
        builder.comment("By default (1.0), players hear shots at a distance between 256 and 512 blocks, depending on the gun type (256 for pistols and SMGs, 384 for shotguns and rifles, 512 for sniper rifles and machine guns).");
        builder.comment("You can disable this feature by setting this to 0");
        BULLET_REFLECTION_FACTOR = builder.defineInRange("BulletReflectionFactor", 1.0, 0.0, Double.MAX_VALUE);

        builder.comment("This value is the the distance from which players will hear or not hear a whizz sound when a bullet pass by.");
        builder.comment("You can disable this feature by setting this to 0");
        BULLET_WHIZZ = builder.defineInRange("BulletWhizz", 5.0, 0.0, Double.MAX_VALUE);

        serverConfig(builder);

        builder.pop();
    }

    /**
     * 这些配置不加入 cloth config api 中
     */
    private static void serverConfig(ForgeConfigSpec.Builder builder) {
        builder.comment("DEV: Server hitbox offset (If the hitbox is ahead, fill in a negative number)");
        SERVER_HITBOX_OFFSET = builder.defineInRange("ServerHitboxOffset", 3, -Double.MAX_VALUE, Double.MAX_VALUE);

        builder.comment("Server hitbox latency fix");
        SERVER_HITBOX_LATENCY_FIX = builder.define("ServerHitboxLatencyFix", true);

        builder.comment("The maximum latency (in milliseconds) for the server hitbox latency fix saved");
        SERVER_HITBOX_LATENCY_MAX_SAVE_MS = builder.defineInRange("ServerHitboxLatencyMaxSaveMs", 1000, 250, Double.MAX_VALUE);
    }
}