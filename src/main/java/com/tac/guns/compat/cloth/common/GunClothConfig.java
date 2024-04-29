package com.tac.guns.compat.cloth.common;

import com.tac.guns.config.common.GunConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.TranslatableComponent;

public class GunClothConfig {
    public static void init(ConfigBuilder root, ConfigEntryBuilder entryBuilder) {
        ConfigCategory gun = root.getOrCreateCategory(new TranslatableComponent("config.tac.common.gun"));

        gun.addEntry(entryBuilder.startIntField(new TranslatableComponent("config.tac.common.gun.default_gun_fire_sound_distance"), GunConfig.DEFAULT_GUN_FIRE_SOUND_DISTANCE.get())
                .setMin(0).setMax(Integer.MAX_VALUE).setDefaultValue(64).setTooltip(new TranslatableComponent("config.tac.common.gun.default_gun_fire_sound_distance.desc"))
                .setSaveConsumer(GunConfig.DEFAULT_GUN_FIRE_SOUND_DISTANCE::set).build());

        gun.addEntry(entryBuilder.startBooleanToggle(new TranslatableComponent("config.tac.common.gun.creative_player_consume_ammo"), GunConfig.CREATIVE_PLAYER_CONSUME_AMMO.get())
                .setDefaultValue(true).setTooltip(new TranslatableComponent("config.tac.common.gun.creative_player_consume_ammo.desc"))
                .setSaveConsumer(GunConfig.CREATIVE_PLAYER_CONSUME_AMMO::set).build());
    }
}
