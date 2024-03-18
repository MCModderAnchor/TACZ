package com.tac.guns.config;

import com.tac.guns.config.sub.AmmoConfig;
import com.tac.guns.config.sub.OtherConfig;
import net.minecraftforge.common.ForgeConfigSpec;

public final class GeneralConfig {
    public static ForgeConfigSpec init() {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        AmmoConfig.init(builder);
        OtherConfig.init(builder);
        return builder.build();
    }
}
