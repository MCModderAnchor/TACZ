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
    public static ForgeConfigSpec.BooleanValue override;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("gunpack");
        builder.comment("When enabled, the mod will not try to overwrite the default pack under .minecraft/tacz\n" +
                "Since 1.0.4, the overwriting will only run when you start client or a dedicated server");
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
        config.save();
    }
}
