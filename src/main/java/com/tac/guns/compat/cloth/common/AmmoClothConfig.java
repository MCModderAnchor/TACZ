package com.tac.guns.compat.cloth.common;

import com.google.common.collect.Lists;
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

        ammo.addEntry(entryBuilder.startStrList(new TranslatableComponent("config.tac.common.ammo.pass_through_blocks"), AmmoConfig.PASS_THROUGH_BLOCKS.get())
                .setDefaultValue(Lists.newArrayList()).setTooltip(new TranslatableComponent("config.tac.common.ammo.pass_through_blocks.desc"))
                .setSaveConsumer(AmmoConfig.PASS_THROUGH_BLOCKS::set).build());

        ammo.addEntry(entryBuilder.startDoubleField(new TranslatableComponent("config.tac.common.ammo.damage_base_multiplier"), AmmoConfig.DAMAGE_BASE_MULTIPLIER.get())
                .setMin(0).setMax(Integer.MAX_VALUE).setDefaultValue(1).setTooltip(new TranslatableComponent("config.tac.common.ammo.damage_base_multiplier.desc"))
                .setSaveConsumer(AmmoConfig.DAMAGE_BASE_MULTIPLIER::set).build());

        ammo.addEntry(entryBuilder.startDoubleField(new TranslatableComponent("config.tac.common.ammo.armor_ignore_base_multiplier"), AmmoConfig.ARMOR_IGNORE_BASE_MULTIPLIER.get())
                .setMin(0).setMax(Integer.MAX_VALUE).setDefaultValue(1).setTooltip(new TranslatableComponent("config.tac.common.ammo.armor_ignore_base_multiplier.desc"))
                .setSaveConsumer(AmmoConfig.ARMOR_IGNORE_BASE_MULTIPLIER::set).build());

        ammo.addEntry(entryBuilder.startDoubleField(new TranslatableComponent("config.tac.common.ammo.head_shot_base_multiplier"), AmmoConfig.HEAD_SHOT_BASE_MULTIPLIER.get())
                .setMin(0).setMax(Integer.MAX_VALUE).setDefaultValue(1).setTooltip(new TranslatableComponent("config.tac.common.ammo.head_shot_base_multiplier.desc"))
                .setSaveConsumer(AmmoConfig.HEAD_SHOT_BASE_MULTIPLIER::set).build());

        ammo.addEntry(entryBuilder.startBooleanToggle(new TranslatableComponent("config.tac.common.ammo.destroy_glass"), AmmoConfig.DESTROY_GLASS.get())
                .setDefaultValue(true).setTooltip(new TranslatableComponent("config.tac.common.ammo.destroy_glass.desc"))
                .setSaveConsumer(AmmoConfig.DESTROY_GLASS::set).build());

        ammo.addEntry(entryBuilder.startBooleanToggle(new TranslatableComponent("config.tac.common.ammo.ignite_block"), AmmoConfig.IGNITE_BLOCK.get())
                .setDefaultValue(true).setTooltip(new TranslatableComponent("config.tac.common.ammo.ignite_block.desc"))
                .setSaveConsumer(AmmoConfig.IGNITE_BLOCK::set).build());
    }
}
