package com.tacz.guns.config;

import com.tacz.guns.config.common.AmmoConfig;
import com.tacz.guns.config.common.GunConfig;
import com.tacz.guns.config.common.OtherConfig;
import net.minecraftforge.common.ForgeConfigSpec;

public final class CommonConfig {
    public static ForgeConfigSpec COMMON_CONFIG_SPEC;

    public static ForgeConfigSpec init() {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        GunConfig.init(builder);
        AmmoConfig.init(builder);
        OtherConfig.init(builder);
        COMMON_CONFIG_SPEC = builder.build();
        return COMMON_CONFIG_SPEC;
    }
}
