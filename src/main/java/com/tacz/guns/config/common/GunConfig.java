package com.tacz.guns.config.common;

import net.minecraftforge.common.ForgeConfigSpec;

public class GunConfig {
    public static ForgeConfigSpec.IntValue DEFAULT_GUN_FIRE_SOUND_DISTANCE;
    public static ForgeConfigSpec.IntValue DEFAULT_GUN_OTHER_SOUND_DISTANCE;
    public static ForgeConfigSpec.BooleanValue CREATIVE_PLAYER_CONSUME_AMMO;
    public static ForgeConfigSpec.BooleanValue AUTO_RELOAD_WHEN_RESPAWN;

    public static void init(ForgeConfigSpec.Builder builder) {
        builder.push("gun");

        builder.comment("The default fire sound range (block)");
        DEFAULT_GUN_FIRE_SOUND_DISTANCE = builder.defineInRange("DefaultGunFireSoundDistance", 64, 0, Integer.MAX_VALUE);

        builder.comment("The range (block) of other gun sound, reloading sound etc.");
        DEFAULT_GUN_OTHER_SOUND_DISTANCE = builder.defineInRange("DefaultGunOtherSoundDistance", 16, 0, Integer.MAX_VALUE);

        builder.comment("Whether or not the player will consume ammo in creative mode");
        CREATIVE_PLAYER_CONSUME_AMMO = builder.define("CreativePlayerConsumeAmmo", true);

        builder.comment("Auto reload all the guns in player inventory, useful for pvp servers");
        AUTO_RELOAD_WHEN_RESPAWN = builder.define("AutoReloadWhenRespawn", false);

        builder.pop();
    }
}
