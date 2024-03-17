package com.tac.guns.config;

import com.tac.guns.config.sub.OtherConfig;
import net.minecraftforge.common.ForgeConfigSpec;

public final class GeneralConfig {
    public static ForgeConfigSpec init() {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        OtherConfig.init(builder);
        return builder.build();
    }
}
