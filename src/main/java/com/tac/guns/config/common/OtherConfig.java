package com.tac.guns.config.common;

import net.minecraftforge.common.ForgeConfigSpec;

public class OtherConfig {
    public static ForgeConfigSpec.IntValue AMMO_BOX_STACK_SIZE;
    public static ForgeConfigSpec.BooleanValue DEFAULT_PACK_DEBUG;

    public static void init(ForgeConfigSpec.Builder builder) {
        builder.push("other");

        builder.comment("The maximum stack size of ammo that the ammo box can hold");
        AMMO_BOX_STACK_SIZE = builder.defineInRange("AmmoBoxStackSize", 5, 1, Integer.MAX_VALUE);

        builder.comment("When enabled, the reload command will not overwrite the default model file under config");
        DEFAULT_PACK_DEBUG = builder.define("DefaultPackDebug", false);

        builder.pop();
    }
}
