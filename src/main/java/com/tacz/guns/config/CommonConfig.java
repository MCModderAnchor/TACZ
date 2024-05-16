package com.tacz.guns.config;

import com.tacz.guns.config.common.AmmoConfig;
import com.tacz.guns.config.common.GunConfig;
import com.tacz.guns.config.common.OtherConfig;
import net.minecraftforge.common.ForgeConfigSpec;

public final class CommonConfig {
    public static ForgeConfigSpec init() {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        GunConfig.init(builder);
        AmmoConfig.init(builder);
        OtherConfig.init(builder);
        return builder.build();
    }
}
