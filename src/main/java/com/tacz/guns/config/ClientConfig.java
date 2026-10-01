package com.tacz.guns.config;

import com.tacz.guns.config.client.*;
import net.neoforged.neoforge.common.ModConfigSpec;

public class ClientConfig {
    public static ModConfigSpec CLIENT_CONFIG_SPEC;

    public static ModConfigSpec init() {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        KeyConfig.init(builder);
        RenderConfig.init(builder);
        ResourceConfig.init(builder);
        SoundConfig.init(builder);
        ZoomConfig.init(builder);
        CLIENT_CONFIG_SPEC = builder.build();
        return CLIENT_CONFIG_SPEC;
    }
}
