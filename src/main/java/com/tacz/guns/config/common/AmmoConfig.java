package com.tacz.guns.config.common;

import com.google.common.collect.Lists;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public class AmmoConfig {
    public static final List<String> DEFAULT_DESTROY_BLOCKS = List.of("#forge:glass", "#forge:glass_panes");
    public static final List<String> DEFAULT_BULLET_DAMAGE_IGNORE_LIST = List.of(
            "minecraft:painting", "minecraft:item_frame", "minecraft:glow_item_frame");

    public static ForgeConfigSpec.BooleanValue EXPLOSIVE_AMMO_DESTROYS_BLOCK;
    public static ForgeConfigSpec.BooleanValue EXPLOSIVE_AMMO_FIRE;
    public static ForgeConfigSpec.BooleanValue EXPLOSIVE_AMMO_KNOCK_BACK;
    public static ForgeConfigSpec.IntValue EXPLOSIVE_AMMO_VISIBLE_DISTANCE;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> PASS_THROUGH_BLOCKS;
    public static ForgeConfigSpec.BooleanValue DESTROY_GLASS;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> DESTROY_BLOCKS;
    public static ForgeConfigSpec.BooleanValue IGNITE_BLOCK;
    public static ForgeConfigSpec.BooleanValue IGNITE_ENTITY;
    public static ForgeConfigSpec.BooleanValue IGNORE_TEAM_FRIENDLY_FIRE;
    public static ForgeConfigSpec.BooleanValue ENABLE_BULLET_DAMAGE_IGNORE_LIST;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> BULLET_DAMAGE_IGNORE_LIST;
    public static ForgeConfigSpec.DoubleValue GLOBAL_BULLET_SPEED_MODIFIER;

    public static void init(ForgeConfigSpec.Builder builder) {
        builder.push("ammo");

        builder.comment("Warning: Ammo with explosive properties can break blocks");
        EXPLOSIVE_AMMO_DESTROYS_BLOCK = builder.define("ExplosiveAmmoDestroysBlock", true);

        builder.comment("Warning: Ammo with explosive properties can set the surroundings on fire");
        EXPLOSIVE_AMMO_FIRE = builder.define("ExplosiveAmmoFire", false);

        builder.comment("Ammo with explosive properties can add knockback effect");
        EXPLOSIVE_AMMO_KNOCK_BACK = builder.define("ExplosiveAmmoKnockBack", true);

        builder.comment("The distance at which the explosion effect can be seen");
        EXPLOSIVE_AMMO_VISIBLE_DISTANCE = builder.defineInRange("ExplosiveAmmoVisibleDistance", 192, 0, Integer.MAX_VALUE);

        builder.comment("Blocks that bullets can pass through. Accepts block IDs and tags prefixed with #, e.g. minecraft:glass or #forge:glass.");
        PASS_THROUGH_BLOCKS = builder.defineListAllowEmpty("PassThroughBlocks", Lists.newArrayList(), value -> value instanceof String);

        builder.comment("Allow bullets to destroy blocks listed in DestroyBlocks without drops. The name is retained for compatibility; the list is not limited to glass.");
        DESTROY_GLASS = builder.define("DestroyGlass", true);

        builder.comment("Blocks that bullets can destroy when DestroyGlass is enabled. Accepts block IDs and tags prefixed with #, e.g. minecraft:glass or #forge:glass.");
        DESTROY_BLOCKS = builder.defineListAllowEmpty("DestroyBlocks", Lists.newArrayList(DEFAULT_DESTROY_BLOCKS), value -> value instanceof String);

        builder.comment("Whether a ammo can ignite the block");
        IGNITE_BLOCK = builder.define("IgniteBlock", true);

        builder.comment("Whether a ammo can ignite the entity");
        IGNITE_ENTITY = builder.define("IgniteEntity", true);

        builder.comment("Bullets pass through members of the shooter's scoreboard team when friendly fire is disabled, without consuming pierce count");
        IGNORE_TEAM_FRIENDLY_FIRE = builder.define("IgnoreTeamFriendlyFire", false);

        builder.comment("Enable the entity bullet damage ignore list. Listed entities are ignored by bullet hit detection and take no damage or knockback from ammo explosions.");
        ENABLE_BULLET_DAMAGE_IGNORE_LIST = builder.define("EnableBulletDamageIgnoreList", false);

        builder.comment("Entity type IDs protected from bullets and ammo explosions when EnableBulletDamageIgnoreList is enabled.");
        BULLET_DAMAGE_IGNORE_LIST = builder.defineListAllowEmpty("BulletDamageIgnoreList", Lists.newArrayList(DEFAULT_BULLET_DAMAGE_IGNORE_LIST),
                value -> value instanceof String id && ResourceLocation.tryParse(id) != null);

        builder.comment("Global bullet speed modifier, the init speed of the bullet will be multiplied by this value, default is 2.0");
        builder.comment("This is to compensate the side effects introduced while fixing the shooter variable input issue");
        GLOBAL_BULLET_SPEED_MODIFIER = builder.defineInRange("GlobalBulletSpeedModifier", 2.0, 0.01, 20);

        builder.pop();
    }
}
