package com.tac.guns.config;

import com.tac.guns.config.common.AmmoConfig;
import com.tac.guns.config.common.OtherConfig;
import net.minecraftforge.common.ForgeConfigSpec;

public final class CommonConfig {
    public static ForgeConfigSpec init() {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        AmmoConfig.init(builder);
        OtherConfig.init(builder);
        return builder.build();
    }
}
