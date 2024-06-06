package com.tacz.guns.compat.cloth.common;

import com.tacz.guns.config.common.GunConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;

public class GunClothConfig {
    public static void init(ConfigBuilder root, ConfigEntryBuilder entryBuilder) {
        ConfigCategory gun = root.getOrCreateCategory(Component.translatable("config.tacz.common.gun"));

        gun.addEntry(entryBuilder.startIntField(Component.translatable("config.tacz.common.gun.default_gun_fire_sound_distance"), GunConfig.DEFAULT_GUN_FIRE_SOUND_DISTANCE.get())
                .setMin(0).setMax(Integer.MAX_VALUE).setDefaultValue(64).setTooltip(Component.translatable("config.tacz.common.gun.default_gun_fire_sound_distance.desc"))
                .setSaveConsumer(GunConfig.DEFAULT_GUN_FIRE_SOUND_DISTANCE::set).build());

        gun.addEntry(entryBuilder.startIntField(Component.translatable("config.tacz.common.gun.default_gun_other_sound_distance"), GunConfig.DEFAULT_GUN_OTHER_SOUND_DISTANCE.get())
                .setMin(0).setMax(Integer.MAX_VALUE).setDefaultValue(16).setTooltip(Component.translatable("config.tacz.common.gun.default_gun_other_sound_distance.desc"))
                .setSaveConsumer(GunConfig.DEFAULT_GUN_OTHER_SOUND_DISTANCE::set).build());

        gun.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.tacz.common.gun.creative_player_consume_ammo"), GunConfig.CREATIVE_PLAYER_CONSUME_AMMO.get())
                .setDefaultValue(true).setTooltip(Component.translatable("config.tacz.common.gun.creative_player_consume_ammo.desc"))
                .setSaveConsumer(GunConfig.CREATIVE_PLAYER_CONSUME_AMMO::set).build());

        gun.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.tacz.common.gun.auto_reload_when_respawn"), GunConfig.AUTO_RELOAD_WHEN_RESPAWN.get())
                .setDefaultValue(false).setTooltip(Component.translatable("config.tacz.common.gun.auto_reload_when_respawn.desc"))
                .setSaveConsumer(GunConfig.AUTO_RELOAD_WHEN_RESPAWN::set).build());
    }
}
