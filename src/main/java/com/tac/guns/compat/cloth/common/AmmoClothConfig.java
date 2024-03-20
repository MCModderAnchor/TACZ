package com.tac.guns.compat.cloth.common;

import com.tac.guns.config.common.AmmoConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.TranslatableComponent;

public class AmmoClothConfig {
    public static void init(ConfigBuilder root, ConfigEntryBuilder entryBuilder) {
        ConfigCategory ammo = root.getOrCreateCategory(new TranslatableComponent("config.tac.common.ammo"));

        ammo.addEntry(entryBuilder.startBooleanToggle(new TranslatableComponent("config.tac.common.ammo.explosive_ammo_destroys_blocks"), AmmoConfig.EXPLOSIVE_AMMO_DESTROYS_BLOCKS.get())
                .setDefaultValue(false).setTooltip(new TranslatableComponent("config.tac.common.ammo.explosive_ammo_destroys_blocks.desc"))
                .setSaveConsumer(AmmoConfig.EXPLOSIVE_AMMO_DESTROYS_BLOCKS::set).build());

        ammo.addEntry(entryBuilder.startBooleanToggle(new TranslatableComponent("config.tac.common.ammo.explosive_ammo_fire"), AmmoConfig.EXPLOSIVE_AMMO_FIRE.get())
                .setDefaultValue(false).setTooltip(new TranslatableComponent("config.tac.common.ammo.explosive_ammo_fire.desc"))
                .setSaveConsumer(AmmoConfig.EXPLOSIVE_AMMO_FIRE::set).build());

        ammo.addEntry(entryBuilder.startBooleanToggle(new TranslatableComponent("config.tac.common.ammo.explosive_ammo_knock_back"), AmmoConfig.EXPLOSIVE_AMMO_KNOCK_BACK.get())
                .setDefaultValue(true).setTooltip(new TranslatableComponent("config.tac.common.ammo.explosive_ammo_knock_back.desc"))
                .setSaveConsumer(AmmoConfig.EXPLOSIVE_AMMO_KNOCK_BACK::set).build());

        ammo.addEntry(entryBuilder.startIntField(new TranslatableComponent("config.tac.common.ammo.explosive_ammo_visible_distance"), AmmoConfig.EXPLOSIVE_AMMO_VISIBLE_DISTANCE.get())
                .setMin(0).setMax(Integer.MAX_VALUE).setDefaultValue(192).setTooltip(new TranslatableComponent("config.tac.common.ammo.explosive_ammo_visible_distance.desc"))
                .setSaveConsumer(AmmoConfig.EXPLOSIVE_AMMO_VISIBLE_DISTANCE::set).build());
    }
}
