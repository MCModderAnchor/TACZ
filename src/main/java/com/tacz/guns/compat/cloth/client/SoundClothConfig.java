package com.tacz.guns.compat.cloth.client;

import com.tacz.guns.config.client.SoundConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;

public class SoundClothConfig {
    public static void init(ConfigBuilder root, ConfigEntryBuilder entryBuilder) {
        ConfigCategory sound = root.getOrCreateCategory(Component.translatable("config.tacz.client.sound"));

        sound.addEntry(entryBuilder.startIntField(Component.translatable("config.tacz.client.sound.hit_sound_concurrency_limit"), SoundConfig.HIT_SOUND_CONCURRENCY_LIMIT.get())
                .setMin(0).setMax(128).setDefaultValue(1).setTooltip(Component.translatable("config.tacz.client.sound.hit_sound_concurrency_limit.desc"))
                .setSaveConsumer(SoundConfig.HIT_SOUND_CONCURRENCY_LIMIT::set).build());

        sound.addEntry(entryBuilder.startIntField(Component.translatable("config.tacz.client.sound.default_sound_concurrency_limit"), SoundConfig.DEFAULT_SOUND_CONCURRENCY_LIMIT.get())
                .setMin(0).setMax(128).setDefaultValue(2).setTooltip(Component.translatable("config.tacz.client.sound.default_sound_concurrency_limit.desc"))
                .setSaveConsumer(SoundConfig.DEFAULT_SOUND_CONCURRENCY_LIMIT::set).build());

        sound.addEntry(entryBuilder.startIntField(Component.translatable("config.tacz.client.sound.high_frequency_sound_concurrency_limit"), SoundConfig.HIGH_FREQUENCY_SOUND_CONCURRENCY_LIMIT.get())
                .setMin(0).setMax(128).setDefaultValue(4).setTooltip(Component.translatable("config.tacz.client.sound.high_frequency_sound_concurrency_limit.desc"))
                .setSaveConsumer(SoundConfig.HIGH_FREQUENCY_SOUND_CONCURRENCY_LIMIT::set).build());

        sound.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.tacz.client.sound.first_person_animation_sound_tracking"), SoundConfig.FIRST_PERSON_ANIMATION_SOUND_TRACKING.get())
                .setDefaultValue(false).setTooltip(Component.translatable("config.tacz.client.sound.first_person_animation_sound_tracking.desc"))
                .setSaveConsumer(SoundConfig.FIRST_PERSON_ANIMATION_SOUND_TRACKING::set).build());
    }
}
