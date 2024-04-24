package com.tac.guns.compat.cloth.common;

import com.google.common.collect.Lists;
import com.tac.guns.config.common.OtherConfig;
import com.tac.guns.event.HeadShotAABBConfigRead;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.TranslatableComponent;

import java.util.List;

public class OtherClothConfig {
    public static void init(ConfigBuilder root, ConfigEntryBuilder entryBuilder) {
        ConfigCategory other = root.getOrCreateCategory(new TranslatableComponent("config.tac.common.other"));

        other.addEntry(entryBuilder.startIntField(new TranslatableComponent("config.tac.common.other.ammo_box_stack_size"), OtherConfig.AMMO_BOX_STACK_SIZE.get())
                .setMin(1).setMax(Integer.MAX_VALUE).setDefaultValue(5).setTooltip(new TranslatableComponent("config.tac.common.other.ammo_box_stack_size.desc"))
                .setSaveConsumer(OtherConfig.AMMO_BOX_STACK_SIZE::set).build());

        other.addEntry(entryBuilder.startBooleanToggle(new TranslatableComponent("config.tac.common.other.default_pack_debug"), OtherConfig.DEFAULT_PACK_DEBUG.get())
                .setDefaultValue(false).setTooltip(new TranslatableComponent("config.tac.common.other.default_pack_debug.desc"))
                .setSaveConsumer(OtherConfig.DEFAULT_PACK_DEBUG::set).build());

        other.addEntry(entryBuilder.startStrList(new TranslatableComponent("config.tac.common.other.head_shot_aabb"), OtherConfig.HEAD_SHOT_AABB.get())
                .setDefaultValue(Lists.newArrayList()).setTooltip(new TranslatableComponent("config.tac.common.other.head_shot_aabb.desc"))
                .setSaveConsumer(OtherClothConfig::setAABBData).build());
    }

    private static void setAABBData(List<String> data) {
        HeadShotAABBConfigRead.clearAABB();
        for (String text : data) {
            HeadShotAABBConfigRead.addCheck(text);
        }
        OtherConfig.HEAD_SHOT_AABB.set(data);
    }
}
