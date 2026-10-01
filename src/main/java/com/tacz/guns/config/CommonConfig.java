package com.tacz.guns.config;

import com.tacz.guns.config.common.AmmoConfig;
import com.tacz.guns.config.common.GunConfig;
import com.tacz.guns.config.common.OtherConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class CommonConfig {
    public static ModConfigSpec COMMON_CONFIG_SPEC;

    public static ModConfigSpec init() {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        GunConfig.init(builder);
        AmmoConfig.init(builder);
        OtherConfig.init(builder);
        COMMON_CONFIG_SPEC = builder.build();
        return COMMON_CONFIG_SPEC;
    }
}
