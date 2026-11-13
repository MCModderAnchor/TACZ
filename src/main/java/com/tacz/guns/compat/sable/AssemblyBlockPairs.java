package com.tacz.guns.compat.sable;

import com.tacz.guns.block.GunSmithTableBlockB;
import com.tacz.guns.block.GunSmithTableBlockC;
import com.tacz.guns.block.StatueBlock;
import com.tacz.guns.block.TargetBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.jetbrains.annotations.Nullable;

public final class AssemblyBlockPairs {
    private AssemblyBlockPairs() {
    }

    @Nullable
    public static BlockPos getOtherHalf(BlockGetter level, BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        if (block instanceof GunSmithTableBlockB) {
            Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            BedPart part = state.getValue(GunSmithTableBlockB.PART);
            BlockPos otherPos = pos.relative(GunSmithTableBlockB.getNeighbourDirection(part, facing));
            BlockState otherState = level.getBlockState(otherPos);
            if (otherState.is(block) && otherState.getValue(GunSmithTableBlockB.PART) != part
                    && otherState.getValue(BlockStateProperties.HORIZONTAL_FACING) == facing) {
                return otherPos;
            }
        } else if (block instanceof StatueBlock || block instanceof TargetBlock || block instanceof GunSmithTableBlockC) {
            DoubleBlockHalf half = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
            BlockPos otherPos = half == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
            BlockState otherState = level.getBlockState(otherPos);
            if (otherState.is(block) && otherState.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) != half
                    && otherState.getValue(BlockStateProperties.HORIZONTAL_FACING) == state.getValue(BlockStateProperties.HORIZONTAL_FACING)) {
                return otherPos;
            }
        }
        return null;
    }
}
