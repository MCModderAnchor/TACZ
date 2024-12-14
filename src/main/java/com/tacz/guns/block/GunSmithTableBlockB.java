package com.tacz.guns.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;

/**
 * 双方块的枪械工作台，2x1x1
 */
public class GunSmithTableBlockB extends AbstractGunSmithTableBlock {
    public static final EnumProperty<BedPart> PART = BlockStateProperties.BED_PART;

    public GunSmithTableBlockB() {
        super();
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, BedPart.FOOT));
    }

    private static Direction getNeighbourDirection(BedPart bedPart, Direction direction) {
        return bedPart == BedPart.FOOT ? direction : direction.getOpposite();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }


    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction direction = context.getHorizontalDirection().getClockWise();
        BlockPos clickedPos = context.getClickedPos();
        BlockPos relative = clickedPos.relative(direction);
        Level level = context.getLevel();
        if (level.getBlockState(relative).canBeReplaced(context) && level.getWorldBorder().isWithinBounds(relative)) {
            return this.defaultBlockState().setValue(FACING, direction);
        }
        return null;
    }

    @Override
    public void setPlacedBy(Level worldIn, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(worldIn, pos, state, placer, stack);
        if (!worldIn.isClientSide) {
            BlockPos relative = pos.relative(state.getValue(FACING));
            worldIn.setBlock(relative, state.setValue(PART, BedPart.HEAD), Block.UPDATE_ALL);
            worldIn.blockUpdated(pos, Blocks.AIR);
            state.updateNeighbourShapes(worldIn, pos, Block.UPDATE_ALL);
        }
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState blockState, Player player) {
        // 用于抑制创造模式下摧毁head方块时foot的掉落
        if (!level.isClientSide && player.isCreative()) {
            BedPart bedPart = blockState.getValue(PART);
            if (bedPart == BedPart.FOOT) {
                BlockPos blockpos = pos.relative(getNeighbourDirection(bedPart, blockState.getValue(FACING)));
                BlockState blockstate = level.getBlockState(blockpos);
                if (blockstate.is(this) && blockstate.getValue(PART) == BedPart.HEAD) {
                    level.setBlock(blockpos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                    level.levelEvent(player, LevelEvent.PARTICLES_DESTROY_BLOCK, blockpos, Block.getId(blockstate));
                }
            }
        }
        super.playerWillDestroy(level, pos, blockState, player);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
        if (direction == getNeighbourDirection(state.getValue(PART), state.getValue(FACING))) {
            return facingState.is(this) && facingState.getValue(PART) != state.getValue(PART) ? state : Blocks.AIR.defaultBlockState();
        } else {
            return super.updateShape(state, direction, facingState, level, currentPos, facingPos);
        }
    }

    @Override
    public boolean isRoot(BlockState blockState) {
        return blockState.getValue(PART).equals(BedPart.FOOT);
    }

    @Override
    public float parseRotation(Direction direction) {
        return 90 - 90 * direction.get2DDataValue();
    }

    @Override
    public BlockPos getRootPos(BlockPos pos, BlockState blockState) {
        return blockState.getValue(PART).equals(BedPart.FOOT) ? pos : pos.relative(getNeighbourDirection(BedPart.HEAD, blockState.getValue(FACING)));
    }
}
