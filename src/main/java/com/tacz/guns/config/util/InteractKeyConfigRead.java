package com.tacz.guns.config.util;

import com.google.common.collect.Lists;
import com.tacz.guns.config.sync.SyncConfig;
import com.tacz.guns.mixin.client.StairBlockAccessor;
import com.tacz.guns.util.InheritanceChecker;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.StringUtils;

import java.util.EnumMap;
import java.util.List;

public class InteractKeyConfigRead {
    private static final EnumMap<Type, List<ResourceLocation>> WHITELIST = new EnumMap<>(Type.class);
    private static final EnumMap<Type, List<ResourceLocation>> BLACKLIST = new EnumMap<>(Type.class);

    public static void init() {
        WHITELIST.clear();
        BLACKLIST.clear();
        handleConfigData(SyncConfig.INTERACT_KEY_WHITELIST_BLOCKS.get(), WHITELIST, Type.BLOCK);
        handleConfigData(SyncConfig.INTERACT_KEY_WHITELIST_ENTITIES.get(), WHITELIST, Type.ENTITY);
        handleConfigData(SyncConfig.INTERACT_KEY_BLACKLIST_BLOCKS.get(), BLACKLIST, Type.BLOCK);
        handleConfigData(SyncConfig.INTERACT_KEY_BLACKLIST_ENTITIES.get(), BLACKLIST, Type.ENTITY);
    }

    public static boolean canInteractBlock(Block block) {
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(block);
        if (blockId == null) {
            return false;
        }
        // 先检查黑名单
        if (BLACKLIST.containsKey(Type.BLOCK) && BLACKLIST.get(Type.BLOCK).contains(blockId)) {
            return false;
        }
        // 再检查白名单
        if (WHITELIST.containsKey(Type.BLOCK) && WHITELIST.get(Type.BLOCK).contains(blockId)) {
            return true;
        }
        // 再检查默认情况
        if (block instanceof StairBlockAccessor stair) {
            // 楼梯的情况比较特殊，所以需要单独判断
            return InheritanceChecker.BLOCK_INHERITANCE_CHECKER.isInherited(stair.invokeGetModelBlock().getClass());
        } else {
            return InheritanceChecker.BLOCK_INHERITANCE_CHECKER.isInherited(block.getClass());
        }
    }

    public static boolean canInteractEntity(Entity entity) {
        ResourceLocation entityId = ForgeRegistries.ENTITIES.getKey(entity.getType());
        if (entityId == null) {
            return false;
        }
        // 先检查黑名单
        if (BLACKLIST.containsKey(Type.ENTITY) && BLACKLIST.get(Type.ENTITY).contains(entityId)) {
            return false;
        }
        // 再检查白名单
        if (WHITELIST.containsKey(Type.ENTITY) && WHITELIST.get(Type.ENTITY).contains(entityId)) {
            return true;
        }
        // 再检查默认情况
        if (entity instanceof Mob mob) {
            return InheritanceChecker.MOB_INHERITANCE_CHECKER.isInherited(mob.getClass());
        } else {
            return InheritanceChecker.ENTITY_INHERITANCE_CHECKER.isInherited(entity.getClass());
        }
    }

    private static void handleConfigData(List<String> configData, EnumMap<Type, List<ResourceLocation>> storeList, Type type) {
        configData.forEach(data -> {
            if (data.isEmpty()) {
                return;
            }
            if (StringUtils.isBlank(data)) {
                return;
            }
            ResourceLocation id = new ResourceLocation(data);
            storeList.computeIfAbsent(type, t -> Lists.newArrayList()).add(id);
        });
    }

    public enum Type {
        BLOCK, ENTITY;
    }
}
