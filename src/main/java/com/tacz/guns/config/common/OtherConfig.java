package com.tacz.guns.config.common;

import com.google.common.collect.Lists;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public class OtherConfig {
    public static ForgeConfigSpec.IntValue AMMO_BOX_STACK_SIZE;
    public static ForgeConfigSpec.BooleanValue DEFAULT_PACK_DEBUG;
    public static ForgeConfigSpec.ConfigValue<List<String>> HEAD_SHOT_AABB;
    public static ForgeConfigSpec.IntValue TARGET_SOUND_DISTANCE;
    public static ForgeConfigSpec.DoubleValue SERVER_HITBOX_OFFSET;
    public static ForgeConfigSpec.BooleanValue SERVER_HITBOX_LATENCY_FIX;
    public static ForgeConfigSpec.DoubleValue SERVER_HITBOX_LATENCY_MAX_SAVE_MS;
    public static ForgeConfigSpec.ConfigValue<List<List<String>>> CLIENT_GUN_PACK_DOWNLOAD_URLS;

    public static ForgeConfigSpec.ConfigValue<List<String>> INTERACT_KEY_WHITELIST_BLOCKS;
    public static ForgeConfigSpec.ConfigValue<List<String>> INTERACT_KEY_WHITELIST_ENTITIES;
    public static ForgeConfigSpec.ConfigValue<List<String>> INTERACT_KEY_BLACKLIST_BLOCKS;
    public static ForgeConfigSpec.ConfigValue<List<String>> INTERACT_KEY_BLACKLIST_ENTITIES;

    public static void init(ForgeConfigSpec.Builder builder) {
        builder.push("other");

        builder.comment("The maximum stack size of ammo that the ammo box can hold");
        AMMO_BOX_STACK_SIZE = builder.defineInRange("AmmoBoxStackSize", 5, 1, Integer.MAX_VALUE);

        builder.comment("When enabled, the reload command will not overwrite the default model file under config");
        DEFAULT_PACK_DEBUG = builder.define("DefaultPackDebug", false);

        builder.comment("The entity's head hitbox during the headshot");
        builder.comment("Format: touhou_little_maid:maid [-0.5, 1.0, -0.5, 0.5, 1.5, 0.5]");
        HEAD_SHOT_AABB = builder.define("HeadShotAABB", Lists.newArrayList());

        builder.comment("The farthest sound distance of the target, including minecarts type");
        TARGET_SOUND_DISTANCE = builder.defineInRange("TargetSoundDistance", 128, 0, Integer.MAX_VALUE);

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

        builder.comment("The gun pack that the client player needs to download, needs to fill in the URL and the SHA1 value of the file");
        CLIENT_GUN_PACK_DOWNLOAD_URLS = builder.define("ClientGunPackDownloadUrls", Lists.newArrayList());

        builder.comment("These whitelist blocks can be interacted with when the interact key is pressed");
        INTERACT_KEY_WHITELIST_BLOCKS = builder.define("InteractKeyWhitelistBlocks", Lists.newArrayList("minecraft:chest", "minecraft:trapped_chest", "minecraft:ender_chest"));

        builder.comment("These whitelist entities can be interacted with when the interact key is pressed");
        INTERACT_KEY_WHITELIST_ENTITIES = builder.define("InteractKeyWhitelistEntities", Lists.newArrayList("minecraft:minecart", "minecraft:boat", "minecraft:horse"));

        builder.comment("These blacklist blocks can be interacted with when the interact key is pressed");
        INTERACT_KEY_BLACKLIST_BLOCKS = builder.define("InteractKeyBlacklistBlocks", Lists.newArrayList());

        builder.comment("These blacklist entities can be interacted with when the interact key is pressed");
        INTERACT_KEY_BLACKLIST_ENTITIES = builder.define("InteractKeyBlacklistEntities", Lists.newArrayList());
    }
}