package com.tac.guns.config;

import com.tac.guns.config.client.KeyConfig;
import com.tac.guns.config.client.RenderConfig;
import com.tac.guns.config.client.ZoomConfig;
import net.minecraftforge.common.ForgeConfigSpec;

public class ClientConfig {
    public static ForgeConfigSpec init() {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        KeyConfig.init(builder);
        RenderConfig.init(builder);
        ZoomConfig.init(builder);
        return builder.build();
    }
}
