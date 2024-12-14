package com.tacz.guns.compat.cloth.client;

import com.tacz.guns.compat.cloth.widget.OpenGunPackDirEntry;
import com.tacz.guns.config.client.KeyConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;

public class KeyClothConfig {
    public static void init(ConfigBuilder root, ConfigEntryBuilder entryBuilder) {
        ConfigCategory key = root.getOrCreateCategory(Component.translatable("config.tacz.client.key"));

        key.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.tacz.client.key.hold_to_aim"), KeyConfig.HOLD_TO_AIM.get())
                .setDefaultValue(true).setTooltip(Component.translatable("config.tacz.client.key.hold_to_aim.desc"))
                .setSaveConsumer(KeyConfig.HOLD_TO_AIM::set).build());

        key.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.tacz.client.key.hold_to_crawl"), KeyConfig.HOLD_TO_CRAWL.get())
                .setDefaultValue(true).setTooltip(Component.translatable("config.tacz.client.key.hold_to_crawl.desc"))
                .setSaveConsumer(KeyConfig.HOLD_TO_CRAWL::set).build());

        key.addEntry(new OpenGunPackDirEntry(Component.literal("test")));
    }
}
