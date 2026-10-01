package com.tacz.guns.config;

import com.tacz.guns.config.sync.SyncConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public class ServerConfig {
    /**
     * 因为 Forge 配置文件的加载时间窗口问题，导致有些地方会提前调用配置文件，故缓存一下检查是否已经加载了
     */
    public static ModConfigSpec spec;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        SyncConfig.init(builder);
        spec = builder.build();
    }
}
