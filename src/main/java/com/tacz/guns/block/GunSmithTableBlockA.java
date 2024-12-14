package com.tacz.guns.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 单方块的枪械工作台
 */
public class GunSmithTableBlockA extends AbstractGunSmithTableBlock {
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction direction = context.getHorizontalDirection().getOpposite();
        return this.defaultBlockState().setValue(FACING, direction);
    }

    @Override
    public boolean isRoot(BlockState blockState) {
        return true;
    }

    @Override
    public BlockPos getRootPos(BlockPos pos, BlockState blockState) {
        return pos;
    }
}
