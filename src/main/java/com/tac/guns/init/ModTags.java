package com.tac.guns.init;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class ModTags {
    public static final TagKey<Block> bullet_ignore =
            BlockTags.create(new ResourceLocation("tac:bullet_ignore"));

    public static void init() {
    }
}