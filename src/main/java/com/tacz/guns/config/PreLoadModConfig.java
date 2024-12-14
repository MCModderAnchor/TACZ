package com.tacz.guns.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.config.IConfigEvent;
import net.minecraftforge.fml.config.IConfigSpec;
import net.minecraftforge.fml.config.ModConfig;

import java.nio.file.Path;

public class PreLoadModConfig extends ModConfig {
    private CommentedConfig configData;
    private final ModContainer container;

    public PreLoadModConfig(Type type, IConfigSpec<?> spec, ModContainer container, String fileName) {
        super(type, spec, container, fileName);
        this.container = container;
    }

    public CommentedConfig getConfigData() {
        return this.configData;
    }

    public void setConfigData(final CommentedConfig configData) {
        this.configData = configData;
        this.getSpec().acceptConfig(this.configData);
    }

    public void fireEvent(final IConfigEvent configEvent) {
        this.container.dispatchConfigEvent(configEvent);
    }

    public void save() {
        ((CommentedFileConfig)this.configData).save();
    }

    public Path getFullPath() {
        return ((CommentedFileConfig)this.configData).getNioPath();
    }
}
