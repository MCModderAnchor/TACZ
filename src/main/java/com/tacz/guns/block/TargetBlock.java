package com.tacz.guns.block;

import com.mojang.authlib.GameProfile;
import com.tacz.guns.block.entity.TargetBlockEntity;
import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class TargetBlock extends BaseEntityBlock {
    public static final IntegerProperty OUTPUT_POWER = BlockStateProperties.POWER;
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final BooleanProperty STAND = BooleanProperty.create("stand");
    public static final VoxelShape BOX_BOTTOM_STAND_X = Shapes.or(Block.box(6, 0, 6, 10, 16, 10), Block.box(6, 13, 2, 10, 16, 14));
    public static final VoxelShape BOX_BOTTOM_STAND_Z = Shapes.or(Block.box(6, 0, 6, 10, 16, 10), Block.box(2, 13, 6, 14, 16, 10));
    public static final VoxelShape BOX_BOTTOM_DOWN = Block.box(6, 0, 6, 10, 4, 10);
    public static final VoxelShape BOX_UPPER_X = Block.box(6, 0, 2, 10, 16, 14);
    public static final VoxelShape BOX_UPPER_Z = Block.box(2, 0, 6, 14, 16, 10);

    public TargetBlock() {
        super(Properties.of().sound(SoundType.WOOD).strength(2.0F, 3.0F).noOcclusion());
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HALF, DoubleBlockHalf.LOWER).setValue(STAND, true).setValue(OUTPUT_POWER, 0));
    }

    public static int getRedstoneStrength(BlockHitResult hit, boolean isUpperBlock) {
        // 击中下方，恒为 1
        if (!isUpperBlock) {
            return 1;
        }
        Vec3 hitLocation = hit.getLocation();
        Direction direction = hit.getDirection();
        // 标靶中心为 (0.5, 0.32, 0.5)
        double x = Math.abs(Mth.frac(hitLocation.x) - 0.5);
        double y = Math.abs(Mth.frac(hitLocation.y) - 0.32);
        double z = Math.abs(Mth.frac(hitLocation.z) - 0.5);
        Direction.Axis axis = direction.getAxis();
        double distance;
        if (axis == Direction.Axis.Y) {
            distance = Math.max(x, z);
        } else if (axis == Direction.Axis.Z) {
            distance = Math.max(x, y);
        } else {
            distance = Math.max(y, z);
        }
        // 离开中心 0.25 单位就是最低分？
        double percent = Mth.clamp((0.25 - distance) / 0.25, 0, 1);
        return Math.max(1, Mth.ceil(15 * percent));
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return state.getValue(HALF).equals(DoubleBlockHalf.LOWER) && level.isClientSide() ? createTickerHelper(blockEntityType, ModBlocks.TARGET_BE.get(), TargetBlockEntity::clientTick) : null;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF, STAND, OUTPUT_POWER);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState blockState) {
        if (blockState.getValue(HALF).equals(DoubleBlockHalf.LOWER)) {
            return new TargetBlockEntity(pos, blockState);
        } else {
            return null;
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        boolean stand = state.getValue(STAND);
        boolean axis = state.getValue(FACING).getAxis().equals(Direction.Axis.X);
        if (state.getValue(HALF).equals(DoubleBlockHalf.UPPER)) {
            return stand ? (axis ? BOX_UPPER_X : BOX_UPPER_Z) : Shapes.empty();
        }
        return stand ? (axis ? BOX_BOTTOM_STAND_X : BOX_BOTTOM_STAND_Z) : BOX_BOTTOM_DOWN;
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // 计划刻的内容
        if (!state.getValue(STAND)) {
            level.setBlock(pos, state.setValue(STAND, true).setValue(OUTPUT_POWER, 0), Block.UPDATE_ALL);
        }
    }

    @Override
    public void onProjectileHit(Level world, BlockState state, BlockHitResult hit, Projectile projectile) {
        if (hit.getDirection().getOpposite().equals(state.getValue(FACING))) {
            if (state.getValue(HALF).equals(DoubleBlockHalf.LOWER)) {
                world.getBlockEntity(hit.getBlockPos(), TargetBlockEntity.TYPE).ifPresent(e -> e.hit(world, state, hit, false));
            } else if (state.getValue(HALF).equals(DoubleBlockHalf.UPPER)) {
                world.getBlockEntity(hit.getBlockPos().below(), TargetBlockEntity.TYPE).ifPresent(e -> e.hit(world, state, hit, true));
            }

            if (!world.isClientSide() && projectile.getOwner() instanceof Player player && state.getValue(STAND)) {
                if (projectile instanceof EntityKineticBullet bullet) {
                    String formattedDamage = String.format("%.1f", bullet.getDamage(hit.getLocation()));
                    String formattedDistance = String.format("%.2f", hit.getLocation().distanceTo(player.position()));
                    player.displayClientMessage(Component.translatable("message.tacz.target_minecart.hit", formattedDamage, formattedDistance), true);
                }

            }
        }
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
        DoubleBlockHalf half = state.getValue(HALF);
        boolean stand = state.getValue(STAND);

        if (facing.getAxis() == Direction.Axis.Y) {
            if (half.equals(DoubleBlockHalf.LOWER) && facing == Direction.UP || half.equals(DoubleBlockHalf.UPPER) && facing == Direction.DOWN) {
                // 拆一半另外一半跟着没
                if (!facingState.is(this)) {
                    return Blocks.AIR.defaultBlockState();
                }
                // 同步击倒状态
                if (facingState.getValue(STAND) != stand) {
                    return state.setValue(STAND, facingState.getValue(STAND)).setValue(OUTPUT_POWER, facingState.getValue(OUTPUT_POWER));
                }
            }
        }

        // 底下方块没了也拆掉
        if (half == DoubleBlockHalf.LOWER && facing == Direction.DOWN && !state.canSurvive(level, currentPos)) {
            return Blocks.AIR.defaultBlockState();
        } else {
            return state;
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction direction = context.getHorizontalDirection();
        BlockPos clickedPos = context.getClickedPos();
        BlockPos above = clickedPos.above();
        Level level = context.getLevel();
        if (level.getBlockState(above).canBeReplaced(context) && level.getWorldBorder().isWithinBounds(above)) {
            return this.defaultBlockState().setValue(FACING, direction);
        }
        return null;
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        if (!world.isClientSide) {
            BlockPos above = pos.above();
            world.setBlock(above, state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
            world.blockUpdated(pos, Blocks.AIR);
            state.updateNeighbourShapes(world, pos, Block.UPDATE_ALL);
            if (stack.hasCustomHoverName()) {
                BlockEntity blockentity = world.getBlockEntity(pos);
                if (blockentity instanceof TargetBlockEntity e) {
                    GameProfile gameprofile = new GameProfile(null, stack.getHoverName().getString());
                    e.setOwner(gameprofile);
                    e.setCustomName(stack.getHoverName());
                    e.refresh();
                }
            }
        }
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player) {
        BlockPos blockPos = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
        BlockEntity blockentity = level.getBlockEntity(blockPos);
        if (blockentity instanceof TargetBlockEntity e) {
            return new ItemStack(this).setHoverName(e.getCustomName());
        }
        return super.getCloneItemStack(state, target, level, pos, player);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos blockpos = pos.below();
        BlockState blockstate = level.getBlockState(blockpos);
        if (state.getValue(HALF) == DoubleBlockHalf.LOWER) {
            return true;
        }
        return blockstate.is(this);
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState blockState, BlockGetter blockAccess, BlockPos pos, Direction side) {
        return blockState.getValue(OUTPUT_POWER);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!level.isClientSide() && !state.is(oldState.getBlock())) {
            if (state.getValue(OUTPUT_POWER) > 0 && !level.getBlockTicks().hasScheduledTick(pos, this)) {
                level.setBlock(pos, state.setValue(OUTPUT_POWER, 0), Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_CLIENTS);
            }
        }
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.DESTROY;
    }
}
