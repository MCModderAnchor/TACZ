package com.tac.guns.init;

import com.tac.guns.GunMod;
import com.tac.guns.block.GunSmithTableBlock;
import com.tac.guns.block.entity.GunSmithTableBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, GunMod.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> TILE_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITIES, GunMod.MOD_ID);

    public static RegistryObject<Block> GUN_SMITH_TABLE = BLOCKS.register("gun_smith_table", GunSmithTableBlock::new);
    public static RegistryObject<BlockEntityType<GunSmithTableBlockEntity>> GUN_SMITH_TABLE_BE = TILE_ENTITIES.register("gun_smith_table", () -> GunSmithTableBlockEntity.TYPE);
}
