package com.tac.guns.util.explosion;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface IExplosionDamageable {
    void onProjectileExploded(Level world, BlockState state, BlockPos pos, Entity entity);
}