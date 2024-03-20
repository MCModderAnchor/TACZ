package com.tac.guns.compat.cloth.client;

import com.tac.guns.config.client.RenderConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.TranslatableComponent;

public class RenderClothConfig {
    public static void init(ConfigBuilder root, ConfigEntryBuilder entryBuilder) {
        ConfigCategory render = root.getOrCreateCategory(new TranslatableComponent("config.tac.client.render"));

        render.addEntry(entryBuilder.startIntField(new TranslatableComponent("config.tac.client.render.bullet_hole_particle_life"), RenderConfig.BULLET_HOLE_PARTICLE_LIFE.get())
                .setMin(0).setMax(Integer.MAX_VALUE).setDefaultValue(400).setTooltip(new TranslatableComponent("config.tac.client.render.bullet_hole_particle_life.desc"))
                .setSaveConsumer(i -> RenderConfig.BULLET_HOLE_PARTICLE_LIFE.set(i)).build());

        render.addEntry(entryBuilder.startDoubleField(new TranslatableComponent("config.tac.client.render.bullet_hole_particle_fade_threshold"), RenderConfig.BULLET_HOLE_PARTICLE_FADE_THRESHOLD.get())
                .setMin(0).setMax(1).setDefaultValue(0.98).setTooltip(new TranslatableComponent("config.tac.client.render.bullet_hole_particle_fade_threshold.desc"))
                .setSaveConsumer(i -> RenderConfig.BULLET_HOLE_PARTICLE_FADE_THRESHOLD.set(i)).build());
    }
}
