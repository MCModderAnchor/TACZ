package com.tacz.guns.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.IConfigEvent;
import net.minecraftforge.fml.config.ModConfig;

import java.nio.file.Path;

public class PreLoadConfig {
    private static ForgeConfigSpec spec;
    private static PreLoadModConfig loadedConfig;
    public static ForgeConfigSpec.BooleanValue override;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("gunpack");
        builder.comment("Enable to preserve local default gun pack edits.",
                "When disabled, changes to the TACZ version or bundled resources trigger replacement at startup.",
                "To regenerate, disable this option, delete <game directory>/tacz/tacz_default_gun, then restart.");
        override = builder.define("DefaultPackDebug", false);
        builder.pop();
        spec = builder.build();
    }

    public static PreLoadModConfig getModConfig() {
        ModLoadingContext ctx = ModLoadingContext.get();
        var c = new PreLoadModConfig(ModConfig.Type.COMMON, spec, ctx.getActiveContainer(), "tacz-pre.toml");
        // 从 ConfigTracker 中移除，防止从默认文件夹重复加载
        ConfigTracker.INSTANCE.configSets().get(ModConfig.Type.COMMON).remove(c);
        ConfigTracker.INSTANCE.fileMap().remove(c.getFileName(), c);
        return c;
    }

    public static void load(Path configBasePath) {
        if (spec.isLoaded()) return;
        PreLoadModConfig config = getModConfig();
        final CommentedFileConfig configData = config.getHandler().reader(configBasePath).apply(config);
        config.setConfigData(configData);
        config.fireEvent(IConfigEvent.loading(config));
        loadedConfig = config;
        config.save();
    }

    public static void save() {
        if (loadedConfig != null) {
            loadedConfig.save();
        }
    }
}
