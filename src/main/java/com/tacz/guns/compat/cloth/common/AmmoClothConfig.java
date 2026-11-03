package com.tacz.guns.compat.cloth.common;

import com.google.common.collect.Lists;
import com.tacz.guns.config.common.AmmoConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;

public class AmmoClothConfig {
    public static void init(ConfigBuilder root, ConfigEntryBuilder entryBuilder) {
        ConfigCategory ammo = root.getOrCreateCategory(Component.translatable("config.tacz.common.ammo"));

        ammo.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.tacz.common.ammo.explosive_ammo_destroys_blocks"), AmmoConfig.EXPLOSIVE_AMMO_DESTROYS_BLOCK.get())
                .setDefaultValue(false).setTooltip(Component.translatable("config.tacz.common.ammo.explosive_ammo_destroys_blocks.desc"))
                .setSaveConsumer(AmmoConfig.EXPLOSIVE_AMMO_DESTROYS_BLOCK::set).build());

        ammo.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.tacz.common.ammo.explosive_ammo_fire"), AmmoConfig.EXPLOSIVE_AMMO_FIRE.get())
                .setDefaultValue(false).setTooltip(Component.translatable("config.tacz.common.ammo.explosive_ammo_fire.desc"))
                .setSaveConsumer(AmmoConfig.EXPLOSIVE_AMMO_FIRE::set).build());

        ammo.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.tacz.common.ammo.explosive_ammo_knock_back"), AmmoConfig.EXPLOSIVE_AMMO_KNOCK_BACK.get())
                .setDefaultValue(true).setTooltip(Component.translatable("config.tacz.common.ammo.explosive_ammo_knock_back.desc"))
                .setSaveConsumer(AmmoConfig.EXPLOSIVE_AMMO_KNOCK_BACK::set).build());

        ammo.addEntry(entryBuilder.startIntField(Component.translatable("config.tacz.common.ammo.explosive_ammo_visible_distance"), AmmoConfig.EXPLOSIVE_AMMO_VISIBLE_DISTANCE.get())
                .setMin(0).setMax(Integer.MAX_VALUE).setDefaultValue(192).setTooltip(Component.translatable("config.tacz.common.ammo.explosive_ammo_visible_distance.desc"))
                .setSaveConsumer(AmmoConfig.EXPLOSIVE_AMMO_VISIBLE_DISTANCE::set).build());

        ammo.addEntry(entryBuilder.startStrList(Component.translatable("config.tacz.common.ammo.pass_through_blocks"), Lists.newArrayList(AmmoConfig.PASS_THROUGH_BLOCKS.get()))
                .setDefaultValue(Lists.newArrayList()).setTooltip(Component.translatable("config.tacz.common.ammo.pass_through_blocks.desc"))
                .setSaveConsumer(AmmoConfig.PASS_THROUGH_BLOCKS::set).build());

        ammo.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.tacz.common.ammo.destroy_glass"), AmmoConfig.DESTROY_GLASS.get())
                .setDefaultValue(true).setTooltip(Component.translatable("config.tacz.common.ammo.destroy_glass.desc"))
                .setSaveConsumer(AmmoConfig.DESTROY_GLASS::set).build());

        ammo.addEntry(entryBuilder.startStrList(Component.translatable("config.tacz.common.ammo.destroy_blocks"), Lists.newArrayList(AmmoConfig.DESTROY_BLOCKS.get()))
                .setDefaultValue(Lists.newArrayList(AmmoConfig.DEFAULT_DESTROY_BLOCKS)).setTooltip(Component.translatable("config.tacz.common.ammo.destroy_blocks.desc"))
                .setSaveConsumer(AmmoConfig.DESTROY_BLOCKS::set).build());

        ammo.addEntry(entryBuilder.startDoubleField(Component.translatable("config.tacz.common.ammo.destroy_block_hardness_threshold"), AmmoConfig.DESTROY_BLOCK_HARDNESS_THRESHOLD.get())
                .setMin(0).setMax(Double.MAX_VALUE).setDefaultValue(4).setTooltip(Component.translatable("config.tacz.common.ammo.destroy_block_hardness_threshold.desc"))
                .setSaveConsumer(AmmoConfig.DESTROY_BLOCK_HARDNESS_THRESHOLD::set).build());

        ammo.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.tacz.common.ammo.ignite_block"), AmmoConfig.IGNITE_BLOCK.get())
                .setDefaultValue(true).setTooltip(Component.translatable("config.tacz.common.ammo.ignite_block.desc"))
                .setSaveConsumer(AmmoConfig.IGNITE_BLOCK::set).build());

        ammo.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.tacz.common.ammo.ignite_entity"), AmmoConfig.IGNITE_ENTITY.get())
                .setDefaultValue(true).setTooltip(Component.translatable("config.tacz.common.ammo.ignite_entity.desc"))
                .setSaveConsumer(AmmoConfig.IGNITE_ENTITY::set).build());

        ammo.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.tacz.common.ammo.ignore_team_friendly_fire"), AmmoConfig.IGNORE_TEAM_FRIENDLY_FIRE.get())
                .setDefaultValue(false).setTooltip(Component.translatable("config.tacz.common.ammo.ignore_team_friendly_fire.desc"))
                .setSaveConsumer(AmmoConfig.IGNORE_TEAM_FRIENDLY_FIRE::set).build());

        ammo.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.tacz.common.ammo.enable_bullet_damage_ignore_list"), AmmoConfig.ENABLE_BULLET_DAMAGE_IGNORE_LIST.get())
                .setDefaultValue(false).setTooltip(Component.translatable("config.tacz.common.ammo.enable_bullet_damage_ignore_list.desc"))
                .setSaveConsumer(AmmoConfig.ENABLE_BULLET_DAMAGE_IGNORE_LIST::set).build());

        ammo.addEntry(entryBuilder.startStrList(Component.translatable("config.tacz.common.ammo.bullet_damage_ignore_list"), Lists.newArrayList(AmmoConfig.BULLET_DAMAGE_IGNORE_LIST.get()))
                .setDefaultValue(Lists.newArrayList(AmmoConfig.DEFAULT_BULLET_DAMAGE_IGNORE_LIST)).setTooltip(Component.translatable("config.tacz.common.ammo.bullet_damage_ignore_list.desc"))
                .setSaveConsumer(AmmoConfig.BULLET_DAMAGE_IGNORE_LIST::set).build());

        ammo.addEntry(entryBuilder.startDoubleField(Component.translatable("config.tacz.common.ammo.global_speed_modifier"), AmmoConfig.GLOBAL_BULLET_SPEED_MODIFIER.get())
                .setDefaultValue(2.0).setTooltip(Component.translatable("config.tacz.common.ammo.global_speed_modifier.desc"))
                .setSaveConsumer(AmmoConfig.GLOBAL_BULLET_SPEED_MODIFIER::set).build());
    }
}
