package com.tacz.guns.config.util;

import com.tacz.guns.GunMod;
import com.tacz.guns.config.common.AmmoConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class AmmoBlockConfigRead {
    private static volatile BlockList passThroughBlocks = new BlockList(Set.of(), List.of());
    private static volatile BlockList destroyBlocks = new BlockList(Set.of(), List.of());

    private AmmoBlockConfigRead() {
    }

    public static void init() {
        passThroughBlocks = readBlockList(AmmoConfig.PASS_THROUGH_BLOCKS.get(), "PassThroughBlocks");
        destroyBlocks = readBlockList(AmmoConfig.DESTROY_BLOCKS.get(), "DestroyBlocks");
    }

    public static boolean canPassThrough(BlockState state) {
        return passThroughBlocks.matches(state);
    }

    public static boolean canDestroy(BlockState state) {
        return destroyBlocks.matches(state);
    }

    private static BlockList readBlockList(List<? extends String> entries, String configName) {
        Set<Block> blocks = new HashSet<>();
        Set<TagKey<Block>> tags = new LinkedHashSet<>();
        for (String entry : entries) {
            String value = entry.trim();
            boolean isTag = value.startsWith("#");
            String name = isTag ? value.substring(1) : value;
            ResourceLocation id = name.isEmpty() ? null : ResourceLocation.tryParse(name);
            if (id == null) {
                GunMod.LOGGER.warn("Ignoring invalid block ID or tag '{}' in ammo.{}", entry, configName);
                continue;
            }
            if (isTag) {
                // 配置加载时 tag 尚未绑定，缓存 TagKey 可随数据包重载自动匹配新内容。
                tags.add(BlockTags.create(id));
            } else if (ForgeRegistries.BLOCKS.containsKey(id)) {
                blocks.add(ForgeRegistries.BLOCKS.getValue(id));
            } else {
                GunMod.LOGGER.warn("Ignoring unknown block ID '{}' in ammo.{}", entry, configName);
            }
        }
        return new BlockList(Set.copyOf(blocks), List.copyOf(tags));
    }

    private record BlockList(Set<Block> blocks, List<TagKey<Block>> tags) {
        private boolean matches(BlockState state) {
            if (blocks.contains(state.getBlock())) {
                return true;
            }
            for (TagKey<Block> tag : tags) {
                if (state.is(tag)) {
                    return true;
                }
            }
            return false;
        }
    }
}
