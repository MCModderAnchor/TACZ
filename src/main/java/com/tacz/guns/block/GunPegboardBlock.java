package com.tacz.guns.block;

import com.tacz.guns.block.entity.GunPegboardBlockEntity;
import com.tacz.guns.item.GunPegboardItem;
import com.tacz.guns.config.util.GunPegboardItemConfigRead;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GunPegboardBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty DYED = BooleanProperty.create("dyed");
    public static final EnumProperty<PegboardShape> SHAPE = EnumProperty.create("shape", PegboardShape.class);
    private static final VoxelShape NORTH_SHAPE = Block.box(0, 0, 13, 16, 16, 16);
    private static final VoxelShape SOUTH_SHAPE = Block.box(0, 0, 0, 16, 16, 3);
    private static final VoxelShape WEST_SHAPE = Block.box(13, 0, 0, 16, 16, 16);
    private static final VoxelShape EAST_SHAPE = Block.box(0, 0, 0, 3, 16, 16);
    private final boolean glowing;

    public GunPegboardBlock() {
        this(false);
    }

    public GunPegboardBlock(boolean glowing) {
        super(Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL).strength(5.0F, 6.0F).noOcclusion().lightLevel(state -> glowing ? 10 : 0));
        this.glowing = glowing;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(DYED, false).setValue(SHAPE, PegboardShape.STRAIGHT));
    }

    public boolean isGlowing() {
        return glowing;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, DYED, SHAPE);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GunPegboardBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state;
        Direction clickedFace = context.getClickedFace();
        BlockPos target = context.getClickedPos().relative(clickedFace.getOpposite());
        BlockState targetState = context.getLevel().getBlockState(target);
        if (!context.replacingClickedOnBlock() && targetState.getBlock() instanceof GunPegboardBlock) {
            Direction targetFacing = targetState.getValue(FACING);
            if (GunPegboardSlotHelper.hasCorner(targetState)
                    && clickedFace == targetState.getValue(SHAPE).getCornerFacing(targetFacing)) {
                targetFacing = clickedFace;
            }
            Direction playerFacing = context.getHorizontalDirection();
            // 水平视线正对板面时延续朝向，沿板面看时按玩家朝向放置垂直板。
            Direction placedFacing = playerFacing.getAxis() == targetFacing.getAxis()
                    ? targetFacing : playerFacing.getOpposite();
            state = defaultBlockState().setValue(FACING, placedFacing);
        } else if (clickedFace.getAxis() == Direction.Axis.Y) {
            state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        } else {
            state = defaultBlockState().setValue(FACING, clickedFace);
        }
        return state.setValue(SHAPE, getConnectedShape(state, context.getLevel(), context.getClickedPos()));
    }

    private PegboardShape getConnectedShape(BlockState state, BlockGetter level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockState front = level.getBlockState(pos.relative(facing));
        if (front.getBlock() instanceof GunPegboardBlock) {
            Direction otherFacing = front.getValue(FACING);
            // 折面位于其朝向的反侧；该边已有同朝向直板时不能转角。
            if (otherFacing == facing.getCounterClockWise()
                    && !hasStraightConnection(level, pos.relative(facing.getClockWise()), facing)) {
                return PegboardShape.CORNER_LEFT;
            }
            if (otherFacing == facing.getClockWise()
                    && !hasStraightConnection(level, pos.relative(facing.getCounterClockWise()), facing)) {
                return PegboardShape.CORNER_RIGHT;
            }
        }
        return PegboardShape.STRAIGHT;
    }

    private static boolean hasStraightConnection(BlockGetter level, BlockPos pos, Direction facing) {
        BlockState neighbor = level.getBlockState(pos);
        return neighbor.getBlock() instanceof GunPegboardBlock && neighbor.getValue(FACING) == facing;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return state.setValue(SHAPE, getConnectedShape(state, level, pos));
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        if (mirror == Mirror.NONE) return state;
        PegboardShape shape = switch (state.getValue(SHAPE)) {
            case CORNER_LEFT -> PegboardShape.CORNER_RIGHT;
            case CORNER_RIGHT -> PegboardShape.CORNER_LEFT;
            case STRAIGHT -> PegboardShape.STRAIGHT;
        };
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING))).setValue(SHAPE, shape);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).is(Items.DEBUG_STICK)) return InteractionResult.PASS;
        if (player.getItemInHand(hand).getItem() instanceof GunPegboardItem) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        int slot = GunPegboardSlotHelper.getSlot(state, pos, hit.getLocation());
        if (slot < 0) {
            return InteractionResult.PASS;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof GunPegboardBlockEntity pegboard)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ItemStack held = player.getItemInHand(hand);
        ItemStack stored = pegboard.getItem(slot);
        if (stored.isEmpty()) {
            if (!held.isEmpty() && GunPegboardItemConfigRead.canPlace(held) && pegboard.setItem(slot, held)) {
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
                return InteractionResult.CONSUME;
            }
            return InteractionResult.PASS;
        }

        if (pegboard.rotateItem(slot)) {
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape plate = getPlateShape(state.getValue(FACING));
        if (state.getValue(SHAPE) == PegboardShape.STRAIGHT) return plate;
        return Shapes.or(plate, getPlateShape(state.getValue(SHAPE).getCornerFacing(state.getValue(FACING))));
    }

    private static VoxelShape getPlateShape(Direction facing) {
        return switch (facing) {
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            case EAST -> EAST_SHAPE;
            case NORTH -> NORTH_SHAPE;
            default -> Shapes.block();
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public void onRemove(BlockState oldState, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!oldState.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof GunPegboardBlockEntity pegboard) {
                pegboard.dropAllItems();
            }
        }
        super.onRemove(oldState, level, pos, newState, movedByPiston);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        List<ItemStack> drops = super.getDrops(state, builder);
        if (builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof GunPegboardBlockEntity pegboard) {
            for (ItemStack drop : drops) {
                if (drop.is(asItem()) && pegboard.getColor() >= 0 && !pegboard.hasInheritedColor()) {
                    ((DyeableLeatherItem) drop.getItem()).setColor(drop, pegboard.getColor());
                }
            }
        }
        return drops;
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player) {
        ItemStack stack = new ItemStack(asItem());
        if (level.getBlockEntity(pos) instanceof GunPegboardBlockEntity pegboard && pegboard.getColor() >= 0) {
            ((DyeableLeatherItem) stack.getItem()).setColor(stack, pegboard.getColor());
        }
        return stack;
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.DESTROY;
    }
}
