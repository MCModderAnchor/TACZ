package com.tacz.guns.config.sync;

import com.google.common.collect.Lists;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public class SyncConfig {
    // 交互键的判断是在客户端执行的，但是需要服务端来控制
    public static ForgeConfigSpec.ConfigValue<List<String>> INTERACT_KEY_WHITELIST_BLOCKS;
    public static ForgeConfigSpec.ConfigValue<List<String>> INTERACT_KEY_WHITELIST_ENTITIES;
    public static ForgeConfigSpec.ConfigValue<List<String>> INTERACT_KEY_BLACKLIST_BLOCKS;
    public static ForgeConfigSpec.ConfigValue<List<String>> INTERACT_KEY_BLACKLIST_ENTITIES;
    public static ForgeConfigSpec.ConfigValue<List<String>> GUN_PEGBOARD_WHITELIST_ITEMS;
    public static ForgeConfigSpec.ConfigValue<List<String>> GUN_PEGBOARD_BLACKLIST_ITEMS;
    public static ForgeConfigSpec.BooleanValue ENABLE_TABLE_FILTER;
    public static ForgeConfigSpec.IntValue BULLET_HIT_BLOCK_EFFECT_SEND_DISTANCE;
    public static ForgeConfigSpec.BooleanValue SERVER_SHOOT_NETWORK_V;
    public static ForgeConfigSpec.BooleanValue SERVER_SHOOT_COOLDOWN_V;

    // 三个全局系数，用于客户端枪械文本提示，需要同步
    public static ForgeConfigSpec.DoubleValue DAMAGE_BASE_MULTIPLIER;
    public static ForgeConfigSpec.DoubleValue ARMOR_IGNORE_BASE_MULTIPLIER;
    public static ForgeConfigSpec.DoubleValue HEAD_SHOT_BASE_MULTIPLIER;
    public static ForgeConfigSpec.DoubleValue WEIGHT_SPEED_MULTIPLIER;

    // 需要同步到客户端，方便客户端 debug 显示碰撞箱
    public static ForgeConfigSpec.ConfigValue<List<String>> HEAD_SHOT_AABB;
    // 子弹盒存储上限需要客户端显示支持
    public static ForgeConfigSpec.IntValue AMMO_BOX_STACK_SIZE;
    // 客户端需要下载的枪械包
    public static ForgeConfigSpec.ConfigValue<List<List<String>>> CLIENT_GUN_PACK_DOWNLOAD_URLS;
    // 禁用趴下战术动作
    public static ForgeConfigSpec.BooleanValue ENABLE_CRAWL;
    public static ForgeConfigSpec.BooleanValue ENABLE_CRAWL_WITHOUT_GUN;
    // 持枪趴下时的最大仰角和俯角，单位为度，需要同步到客户端。
    public static ForgeConfigSpec.DoubleValue CRAWL_MAX_UP_ANGLE;
    public static ForgeConfigSpec.DoubleValue CRAWL_MAX_DOWN_ANGLE;

    public static void init(ForgeConfigSpec.Builder builder) {
        interactKey(builder);
        gunPegboard(builder);
        baseMultiplier(builder);
        misc(builder);
    }

    public static void interactKey(ForgeConfigSpec.Builder builder) {
        builder.push("interact_key");

        builder.comment("These whitelist blocks can be interacted with when the interact key is pressed");
        INTERACT_KEY_WHITELIST_BLOCKS = builder.define("InteractKeyWhitelistBlocks", Lists.newArrayList());

        builder.comment("These whitelist entities can be interacted with when the interact key is pressed");
        INTERACT_KEY_WHITELIST_ENTITIES = builder.define("InteractKeyWhitelistEntities", Lists.newArrayList());

        builder.comment("These blacklist blocks can be interacted with when the interact key is pressed");
        INTERACT_KEY_BLACKLIST_BLOCKS = builder.define("InteractKeyBlacklistBlocks", Lists.newArrayList());

        builder.comment("These blacklist entities can be interacted with when the interact key is pressed");
        INTERACT_KEY_BLACKLIST_ENTITIES = builder.define("InteractKeyBlacklistEntities", Lists.newArrayList());

        builder.pop();
    }

    private static void gunPegboard(ForgeConfigSpec.Builder builder) {
        builder.push("gun_pegboard");

        builder.comment("Extra item ids allowed on the gun pegboard, in addition to TACZ guns, attachments, ammo, and ammo boxes");
        GUN_PEGBOARD_WHITELIST_ITEMS = builder.define("GunPegboardWhitelistItems", Lists.newArrayList());

        builder.comment("Item ids denied on the gun pegboard. This list takes priority over the default allowed TACZ item types and whitelist");
        GUN_PEGBOARD_BLACKLIST_ITEMS = builder.define("GunPegboardBlacklistItems", Lists.newArrayList());

        builder.pop();
    }

    private static void baseMultiplier(ForgeConfigSpec.Builder builder) {
        builder.push("base_multiplier");

        builder.comment("All base damage number is multiplied by this factor");
        DAMAGE_BASE_MULTIPLIER = builder.defineInRange("DamageBaseMultiplier", 1, 0, Double.MAX_VALUE);

        builder.comment("All armor ignore damage number is multiplied by this factor");
        ARMOR_IGNORE_BASE_MULTIPLIER = builder.defineInRange("ArmorIgnoreBaseMultiplier", 1, 0, Double.MAX_VALUE);

        builder.comment("All head shot damage number is multiplied by this factor");
        HEAD_SHOT_BASE_MULTIPLIER = builder.defineInRange("HeadShotBaseMultiplier", 1, 0, Double.MAX_VALUE);

        builder.comment("The movement speed will decrease per kg of weight. 0.015 means 1.5% speed decrease per kg. Set a negative value to disable this feature");
        WEIGHT_SPEED_MULTIPLIER = builder.defineInRange("WeightSpeedMultiplier", 0.015, -1, Double.MAX_VALUE);

        builder.pop();
    }

    private static void misc(ForgeConfigSpec.Builder builder) {
        builder.push("misc");

        builder.comment("The entity's head hitbox during the headshot");
        builder.comment("Format: touhou_little_maid:maid [-0.5, 1.0, -0.5, 0.5, 1.5, 0.5]");
        HEAD_SHOT_AABB = builder.define("HeadShotAABB", Lists.newArrayList());

        builder.comment("The maximum stack size of ammo that the ammo box can hold");
        AMMO_BOX_STACK_SIZE = builder.defineInRange("AmmoBoxStackSize", 3, 1, Integer.MAX_VALUE);

        builder.comment("Deprecated. Use vanilla server resource pack");
        CLIENT_GUN_PACK_DOWNLOAD_URLS = builder.define("ClientGunPackDownloadUrls", Lists.newArrayList());

        builder.comment("Whether or not players are allowed to use the crawl feature");
        ENABLE_CRAWL = builder.define("EnableCrawl", true);

        builder.comment("Allow players to crawl without holding a TACZ gun. EnableCrawl must also be enabled");
        ENABLE_CRAWL_WITHOUT_GUN = builder.define("EnableCrawlWithoutGun", true);

        builder.comment("Maximum upward look angle in degrees while crawling with a gun. Set both crawl angle limits to 90 for the full vanilla pitch range");
        CRAWL_MAX_UP_ANGLE = builder.defineInRange("CrawlMaxUpAngle", 45.0D, 0.0D, 90.0D);

        builder.comment("Maximum downward look angle in degrees while crawling with a gun");
        CRAWL_MAX_DOWN_ANGLE = builder.defineInRange("CrawlMaxDownAngle", 30.0D, 0.0D, 90.0D);

        builder.comment("Enable the recipe limit of default gunsmith table or not");
        ENABLE_TABLE_FILTER = builder.define("EnableDefaultGunSmithTableFilter", true);

        builder.comment("The farthest distance for the server to send bullet hit block effect packets, 0 means no limit");
        BULLET_HIT_BLOCK_EFFECT_SEND_DISTANCE = builder.defineInRange("BulletHitBlockEffectSendDistance", 128, 0, Integer.MAX_VALUE);

        builder.comment("[Debug Option] Do server-side network check while shooting or not");
        SERVER_SHOOT_NETWORK_V = builder.define("ServerShootNetworkCheck", true);

        builder.comment("[Debug Option] Do server-side shoot cooldown check or not." +
                " WARNING: Close this will disable the shoot cooldown check in server-side at all," +
                " which may lead to potential for cheating." +
                " Only consider to close this when you can't shoot at all sometimes.");
        SERVER_SHOOT_COOLDOWN_V = builder.define("ServerShootCooldownCheck", true);
        builder.pop();
    }
}
