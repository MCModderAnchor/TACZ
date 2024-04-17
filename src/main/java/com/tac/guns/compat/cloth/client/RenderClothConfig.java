package com.tac.guns.compat.cloth.client;

import com.tac.guns.client.renderer.crosshair.CrosshairType;
import com.tac.guns.compat.cloth.widget.CrosshairDropdown;
import com.tac.guns.config.client.RenderConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.TranslatableComponent;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

public class RenderClothConfig {
    public static void init(ConfigBuilder root, ConfigEntryBuilder entryBuilder) {
        ConfigCategory render = root.getOrCreateCategory(new TranslatableComponent("config.tac.client.render"));

        render.addEntry(entryBuilder.startIntField(new TranslatableComponent("config.tac.client.render.gun_lod_render_distance"), RenderConfig.GUN_LOD_RENDER_DISTANCE.get())
                .setMin(0).setMax(Integer.MAX_VALUE).setDefaultValue(0).setTooltip(new TranslatableComponent("config.tac.client.render.gun_lod_render_distance.desc"))
                .setSaveConsumer(RenderConfig.GUN_LOD_RENDER_DISTANCE::set).build());

        render.addEntry(entryBuilder.startIntField(new TranslatableComponent("config.tac.client.render.bullet_hole_particle_life"), RenderConfig.BULLET_HOLE_PARTICLE_LIFE.get())
                .setMin(0).setMax(Integer.MAX_VALUE).setDefaultValue(400).setTooltip(new TranslatableComponent("config.tac.client.render.bullet_hole_particle_life.desc"))
                .setSaveConsumer(RenderConfig.BULLET_HOLE_PARTICLE_LIFE::set).build());

        render.addEntry(entryBuilder.startDoubleField(new TranslatableComponent("config.tac.client.render.bullet_hole_particle_fade_threshold"), RenderConfig.BULLET_HOLE_PARTICLE_FADE_THRESHOLD.get())
                .setMin(0).setMax(1).setDefaultValue(0.98).setTooltip(new TranslatableComponent("config.tac.client.render.bullet_hole_particle_fade_threshold.desc"))
                .setSaveConsumer(RenderConfig.BULLET_HOLE_PARTICLE_FADE_THRESHOLD::set).build());

        render.addEntry(entryBuilder.startDropdownMenu(new TranslatableComponent("config.tac.client.render.crosshair_type"),
                        CrosshairDropdown.of(RenderConfig.CROSSHAIR_TYPE.get()), CrosshairDropdown.of())
                .setSelections(Arrays.stream(CrosshairType.values()).sorted().sorted(Comparator.comparing(CrosshairType::name)).collect(Collectors.toCollection(LinkedHashSet::new)))
                .setDefaultValue(CrosshairType.SQUARE_5).setTooltip(new TranslatableComponent("config.tac.client.render.crosshair_type.desc"))
                .setSaveConsumer(RenderConfig.CROSSHAIR_TYPE::set).build());

        render.addEntry(entryBuilder.startDoubleField(new TranslatableComponent("config.tac.client.render.hit_market_start_position"), RenderConfig.HIT_MARKET_START_POSITION.get())
                .setMin(-1024).setMax(1024).setDefaultValue(4).setTooltip(new TranslatableComponent("config.tac.client.render.hit_market_start_position.desc"))
                .setSaveConsumer(RenderConfig.HIT_MARKET_START_POSITION::set).build());

        render.addEntry(entryBuilder.startBooleanToggle(new TranslatableComponent("config.tac.client.render.head_shot_debug_hitbox"), RenderConfig.HEAD_SHOT_DEBUG_HITBOX.get())
                .setDefaultValue(false).setTooltip(new TranslatableComponent("config.tac.client.render.head_shot_debug_hitbox.desc"))
                .setSaveConsumer(RenderConfig.HEAD_SHOT_DEBUG_HITBOX::set).build());
    }
}
