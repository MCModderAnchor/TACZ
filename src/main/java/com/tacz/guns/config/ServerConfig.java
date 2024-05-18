package com.tacz.guns.config;

import com.tacz.guns.config.sync.SyncConfig;
import net.minecraftforge.common.ForgeConfigSpec;

public class ServerConfig {
    /**
     * 因为 Forge 配置文件的加载时间窗口问题，导致有些地方会提前调用配置文件，故缓存一下检查是否已经加载了
     */
    public static ForgeConfigSpec SERVER_CONFIG_SPEC;

    public static ForgeConfigSpec init() {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        SyncConfig.init(builder);
        SERVER_CONFIG_SPEC = builder.build();
        return SERVER_CONFIG_SPEC;
    }
}
