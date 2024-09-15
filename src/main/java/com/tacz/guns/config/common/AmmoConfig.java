package com.tacz.guns.config.common;

import com.google.common.collect.Lists;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public class AmmoConfig {
    public static ForgeConfigSpec.BooleanValue EXPLOSIVE_AMMO_DESTROYS_BLOCK;
    public static ForgeConfigSpec.BooleanValue EXPLOSIVE_AMMO_FIRE;
    public static ForgeConfigSpec.BooleanValue EXPLOSIVE_AMMO_KNOCK_BACK;
    public static ForgeConfigSpec.IntValue EXPLOSIVE_AMMO_VISIBLE_DISTANCE;
    public static ForgeConfigSpec.ConfigValue<List<String>> PASS_THROUGH_BLOCKS;
    public static ForgeConfigSpec.BooleanValue DESTROY_GLASS;
    public static ForgeConfigSpec.BooleanValue IGNITE_BLOCK;
    public static ForgeConfigSpec.BooleanValue IGNITE_ENTITY;

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

        builder.comment("Those blocks that the ammo can pass through");
        PASS_THROUGH_BLOCKS = builder.define("PassThroughBlocks", Lists.newArrayList());

        builder.comment("Whether a ammo can break the glass");
        DESTROY_GLASS = builder.define("DestroyGlass", true);

        builder.comment("Whether a ammo can ignite the block");
        IGNITE_BLOCK = builder.define("IgniteBlock", true);

        builder.comment("Whether a ammo can ignite the entity");
        IGNITE_ENTITY = builder.define("IgniteEntity", true);

        builder.pop();
    }
}
