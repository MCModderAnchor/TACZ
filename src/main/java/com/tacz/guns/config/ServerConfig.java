package com.tacz.guns.config;

import com.tacz.guns.config.sync.SyncConfig;
import net.minecraftforge.common.ForgeConfigSpec;

public class ServerConfig {
    public static ForgeConfigSpec init() {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        SyncConfig.init(builder);
        return builder.build();
    }
}
