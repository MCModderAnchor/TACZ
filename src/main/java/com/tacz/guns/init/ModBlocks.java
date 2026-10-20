package com.tacz.guns.init;

import com.tacz.guns.GunMod;
import com.tacz.guns.block.*;
import com.tacz.guns.block.entity.*;
import com.tacz.guns.block.entity.GunSmithTableBlockEntity;
import com.tacz.guns.block.entity.StatueBlockEntity;
import com.tacz.guns.block.entity.TargetBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(GunMod.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> TILE_ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, GunMod.MOD_ID);

    // 旧方块就让他独占一个了
    public static DeferredBlock<Block> GUN_SMITH_TABLE = BLOCKS.register("gun_smith_table", GunSmithTableBlockB::new);
    public static DeferredBlock<Block> WORKBENCH_111 = BLOCKS.register("workbench_a", GunSmithTableBlockA::new);
    public static DeferredBlock<Block> WORKBENCH_211 = BLOCKS.register("workbench_b", GunSmithTableBlockB::new);
    public static DeferredBlock<Block> WORKBENCH_121 = BLOCKS.register("workbench_c", GunSmithTableBlockC::new);

    public static net.neoforged.neoforge.registries.DeferredBlock<Block> TARGET = BLOCKS.register("target", TargetBlock::new);
    public static net.neoforged.neoforge.registries.DeferredBlock<Block> STATUE = BLOCKS.register("statue", StatueBlock::new);
    public static net.neoforged.neoforge.registries.DeferredBlock<Block> GUN_PEGBOARD = BLOCKS.register("gun_pegboard", () -> new GunPegboardBlock());
    public static net.neoforged.neoforge.registries.DeferredBlock<Block> GLOW_GUN_PEGBOARD = BLOCKS.register("glow_gun_pegboard", () -> new GunPegboardBlock(true));
    public static net.neoforged.neoforge.registries.DeferredBlock<Block> AMMO_STORAGE = BLOCKS.register("ammo_storage", () -> new AmmoStorageBlock(AmmoStorageBlock.IRON_CAPACITY_STACKS));
    public static net.neoforged.neoforge.registries.DeferredBlock<Block> AMMO_STORAGE_GOLD = BLOCKS.register("ammo_storage_gold", () -> new AmmoStorageBlock(AmmoStorageBlock.GOLD_CAPACITY_STACKS));
    public static net.neoforged.neoforge.registries.DeferredBlock<Block> AMMO_STORAGE_DIAMOND = BLOCKS.register("ammo_storage_diamond", () -> new AmmoStorageBlock(AmmoStorageBlock.DIAMOND_CAPACITY_STACKS));
    public static net.neoforged.neoforge.registries.DeferredBlock<Block> GUN_DISPLAY_CASE = BLOCKS.register("gun_display_case", () -> new GunDisplayCaseBlock());

    public static net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<GunSmithTableBlockEntity>> GUN_SMITH_TABLE_BE = TILE_ENTITIES.register("gun_smith_table", () -> GunSmithTableBlockEntity.TYPE);
    public static net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<TargetBlockEntity>> TARGET_BE = TILE_ENTITIES.register("target", () -> TargetBlockEntity.TYPE);
    public static net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<StatueBlockEntity>> STATUE_BE = TILE_ENTITIES.register("statue", () -> StatueBlockEntity.TYPE);
    public static net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<GunPegboardBlockEntity>> GUN_PEGBOARD_BE = TILE_ENTITIES.register("gun_pegboard", () -> GunPegboardBlockEntity.TYPE);
    public static net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<AmmoStorageBlockEntity>> AMMO_STORAGE_BE = TILE_ENTITIES.register("ammo_storage", () -> AmmoStorageBlockEntity.TYPE);
    public static net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<GunDisplayCaseBlockEntity>> GUN_DISPLAY_CASE_BE = TILE_ENTITIES.register("gun_display_case", () -> GunDisplayCaseBlockEntity.TYPE);
    public static final TagKey<Block> BULLET_IGNORE_BLOCKS = BlockTags.create(ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "bullet_ignore"));
}
