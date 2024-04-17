package com.tac.guns.config.common;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.compress.utils.Lists;

import java.util.List;

public class OtherConfig {
    public static ForgeConfigSpec.IntValue AMMO_BOX_STACK_SIZE;
    public static ForgeConfigSpec.BooleanValue DEFAULT_PACK_DEBUG;
    public static ForgeConfigSpec.ConfigValue<List<String>> HEAD_SHOT_AABB;

    public static void init(ForgeConfigSpec.Builder builder) {
        builder.push("other");

        builder.comment("The maximum stack size of ammo that the ammo box can hold");
        AMMO_BOX_STACK_SIZE = builder.defineInRange("AmmoBoxStackSize", 5, 1, Integer.MAX_VALUE);

        builder.comment("When enabled, the reload command will not overwrite the default model file under config");
        DEFAULT_PACK_DEBUG = builder.define("DefaultPackDebug", false);

        builder.comment("The entity's head hitbox during the headshot");
        builder.comment("Format: touhou_little_maid:maid [-0.5, 1.0, -0.5, 0.5, 1.5, 0.5]");
        HEAD_SHOT_AABB = builder.define("HeadShotAABB", Lists.newArrayList());

        builder.pop();
    }
}
