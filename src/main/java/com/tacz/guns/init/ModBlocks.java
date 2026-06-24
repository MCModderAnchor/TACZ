package com.tacz.guns.init;

import com.tacz.guns.GunMod;
import com.tacz.guns.block.*;
import com.tacz.guns.block.entity.AmmoStorageBlockEntity;
import com.tacz.guns.block.entity.GunDisplayCaseBlockEntity;
import com.tacz.guns.block.entity.GunPegboardBlockEntity;
import com.tacz.guns.block.entity.GunSmithTableBlockEntity;
import com.tacz.guns.block.entity.StatueBlockEntity;
import com.tacz.guns.block.entity.TargetBlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, GunMod.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> TILE_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, GunMod.MOD_ID);

    // 旧方块就让他独占一个了
    public static RegistryObject<Block> GUN_SMITH_TABLE = BLOCKS.register("gun_smith_table", GunSmithTableBlockB::new);
    public static RegistryObject<Block> WORKBENCH_111 = BLOCKS.register("workbench_a", GunSmithTableBlockA::new);
    public static RegistryObject<Block> WORKBENCH_211 = BLOCKS.register("workbench_b", GunSmithTableBlockB::new);
    public static RegistryObject<Block> WORKBENCH_121 = BLOCKS.register("workbench_c", GunSmithTableBlockC::new);

    public static RegistryObject<Block> TARGET = BLOCKS.register("target", TargetBlock::new);
    public static RegistryObject<Block> STATUE = BLOCKS.register("statue", StatueBlock::new);
    public static RegistryObject<Block> GUN_PEGBOARD = BLOCKS.register("gun_pegboard", GunPegboardBlock::new);
    public static RegistryObject<Block> GLOW_GUN_PEGBOARD = BLOCKS.register("glow_gun_pegboard", () -> new GunPegboardBlock(true));
    public static RegistryObject<Block> AMMO_STORAGE = BLOCKS.register("ammo_storage", () -> new AmmoStorageBlock(AmmoStorageBlock.IRON_SLOT_STACKS));
    public static RegistryObject<Block> AMMO_STORAGE_GOLD = BLOCKS.register("ammo_storage_gold", () -> new AmmoStorageBlock(AmmoStorageBlock.GOLD_SLOT_STACKS));
    public static RegistryObject<Block> AMMO_STORAGE_DIAMOND = BLOCKS.register("ammo_storage_diamond", () -> new AmmoStorageBlock(AmmoStorageBlock.DIAMOND_SLOT_STACKS));
    public static RegistryObject<Block> GUN_DISPLAY_CASE = BLOCKS.register("gun_display_case", GunDisplayCaseBlock::new);

    public static RegistryObject<BlockEntityType<GunSmithTableBlockEntity>> GUN_SMITH_TABLE_BE = TILE_ENTITIES.register("gun_smith_table", () -> GunSmithTableBlockEntity.TYPE);
    public static RegistryObject<BlockEntityType<TargetBlockEntity>> TARGET_BE = TILE_ENTITIES.register("target", () -> TargetBlockEntity.TYPE);
    public static RegistryObject<BlockEntityType<StatueBlockEntity>> STATUE_BE = TILE_ENTITIES.register("statue", () -> StatueBlockEntity.TYPE);
    public static RegistryObject<BlockEntityType<GunPegboardBlockEntity>> GUN_PEGBOARD_BE = TILE_ENTITIES.register("gun_pegboard", () -> GunPegboardBlockEntity.TYPE);
    public static RegistryObject<BlockEntityType<AmmoStorageBlockEntity>> AMMO_STORAGE_BE = TILE_ENTITIES.register("ammo_storage", () -> AmmoStorageBlockEntity.TYPE);
    public static RegistryObject<BlockEntityType<GunDisplayCaseBlockEntity>> GUN_DISPLAY_CASE_BE = TILE_ENTITIES.register("gun_display_case", () -> GunDisplayCaseBlockEntity.TYPE);
    public static final TagKey<Block> BULLET_IGNORE_BLOCKS = BlockTags.create(new ResourceLocation(GunMod.MOD_ID, "bullet_ignore"));
}
