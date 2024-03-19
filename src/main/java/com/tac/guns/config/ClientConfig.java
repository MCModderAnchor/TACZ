package com.tac.guns.config;

import com.tac.guns.config.client.RenderConfig;
import net.minecraftforge.common.ForgeConfigSpec;

public class ClientConfig {
    public static ForgeConfigSpec init() {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        RenderConfig.init(builder);
        return builder.build();
    }
}
