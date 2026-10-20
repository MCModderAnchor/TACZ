package com.tacz.guns.block;

import com.tacz.guns.api.item.IGun;
import com.tacz.guns.block.entity.GunDisplayCaseBlockEntity;
import com.tacz.guns.config.util.GunPegboardItemConfigRead;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

public class GunDisplayCaseBlock extends BaseEntityBlock {
    public static final com.mojang.serialization.MapCodec<GunDisplayCaseBlock> CODEC = simpleCodec(GunDisplayCaseBlock::new);
    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LOCKED = BooleanProperty.create("locked");
    public static final BooleanProperty LEFT = BooleanProperty.create("left");
    public static final BooleanProperty RIGHT = BooleanProperty.create("right");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    private static final int MAX_GROUP_TOGGLE = 512;
    private static final int LOCK_UPDATE_FLAGS = Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_CLIENTS;

    private static final VoxelShape NORTH_LOCKED_SHAPE = Block.box(0, 0, 4, 16, 16, 16);
    private static final VoxelShape SOUTH_LOCKED_SHAPE = Block.box(0, 0, 0, 16, 16, 12);
    private static final VoxelShape WEST_LOCKED_SHAPE = Block.box(4, 0, 0, 16, 16, 16);
    private static final VoxelShape EAST_LOCKED_SHAPE = Block.box(0, 0, 0, 12, 16, 16);
    private static final VoxelShape NORTH_BACK_PANEL_SHAPE = Block.box(0, 0, 13, 16, 16, 16);
    private static final VoxelShape SOUTH_BACK_PANEL_SHAPE = Block.box(0, 0, 0, 16, 16, 3);
    private static final VoxelShape WEST_BACK_PANEL_SHAPE = Block.box(13, 0, 0, 16, 16, 16);
    private static final VoxelShape EAST_BACK_PANEL_SHAPE = Block.box(0, 0, 0, 3, 16, 16);
    private static final VoxelShape NORTH_OPEN_SHAPE = Shapes.or(
            NORTH_BACK_PANEL_SHAPE,
            Block.box(0, 0, 4, 1, 16, 16),
            Block.box(15, 0, 4, 16, 16, 16),
            Block.box(0, 0, 4, 16, 1, 16),
            Block.box(0, 15, 4, 16, 16, 16)
    );
    private static final VoxelShape SOUTH_OPEN_SHAPE = Shapes.or(
            SOUTH_BACK_PANEL_SHAPE,
            Block.box(0, 0, 0, 1, 16, 12),
            Block.box(15, 0, 0, 16, 16, 12),
            Block.box(0, 0, 0, 16, 1, 12),
            Block.box(0, 15, 0, 16, 16, 12)
    );
    private static final VoxelShape WEST_OPEN_SHAPE = Shapes.or(
            WEST_BACK_PANEL_SHAPE,
            Block.box(4, 0, 0, 16, 16, 1),
            Block.box(4, 0, 15, 16, 16, 16),
            Block.box(4, 0, 0, 16, 1, 16),
            Block.box(4, 15, 0, 16, 16, 16)
    );
    private static final VoxelShape EAST_OPEN_SHAPE = Shapes.or(
            EAST_BACK_PANEL_SHAPE,
            Block.box(0, 0, 0, 12, 16, 1),
            Block.box(0, 0, 15, 12, 16, 16),
            Block.box(0, 0, 0, 12, 1, 16),
            Block.box(0, 15, 0, 12, 16, 16)
    );

    public GunDisplayCaseBlock() {
        this(Properties.of().mapColor(MapColor.METAL).sound(SoundType.GLASS).strength(0.5F, 1200.0F).noOcclusion());
    }

    public GunDisplayCaseBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LOCKED, true)
                .setValue(LEFT, false)
                .setValue(RIGHT, false)
                .setValue(UP, false)
                .setValue(DOWN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LOCKED, LEFT, RIGHT, UP, DOWN);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GunDisplayCaseBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction clickedFace = context.getClickedFace();
        BlockPos clickedPos = pos.relative(clickedFace.getOpposite());
        BlockState clickedState = level.getBlockState(clickedPos);
        boolean clickedDisplayCase = clickedState.getBlock() instanceof GunDisplayCaseBlock;
        Direction facing = getPlacementFacing(context, clickedState, clickedDisplayCase);
        BlockState state = defaultBlockState().setValue(FACING, facing);
        if (context.isSecondaryUseActive()) {
            return state;
        }
        Set<BlockPos> forceConnectedPositions = clickedDisplayCase ? collectConnectedCases(level, clickedPos, facing) : null;
        return updateConnections(level, pos, state, forceConnectedPositions);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
        BooleanProperty property = propertyForDirection(state.getValue(FACING), direction);
        if (property != null && state.getValue(property) && !connectsTo(state, facingState)) {
            return state.setValue(property, false);
        }
        return state;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && !oldState.is(state.getBlock())) {
            syncConnectedNeighbors(level, pos, state);
        }
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
            BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        InteractionResult result = interact(state, level, pos, player, hand, hit);
        return switch (result) {
            case SUCCESS, SUCCESS_NO_ITEM_USED -> net.minecraft.world.ItemInteractionResult.SUCCESS;
            case CONSUME -> net.minecraft.world.ItemInteractionResult.CONSUME;
            case CONSUME_PARTIAL -> net.minecraft.world.ItemInteractionResult.CONSUME_PARTIAL;
            case FAIL -> net.minecraft.world.ItemInteractionResult.FAIL;
            default -> net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return interact(state, level, pos, player, InteractionHand.MAIN_HAND, hit);
    }

    private InteractionResult interact(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        boolean holdingGun = held.getItem() instanceof IGun;
        if (player.isShiftKeyDown()) {
            if (player.getMainHandItem().getItem() instanceof IGun) {
                return InteractionResult.PASS;
            }
            toggleLockFromInteraction(level, pos, state);
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        if (state.getValue(LOCKED)) {
            return held.isEmpty() || holdingGun ? InteractionResult.sidedSuccess(level.isClientSide()) : InteractionResult.PASS;
        }

        int slot = GunDisplayCaseSlotHelper.getSlot(state, pos, hit.getLocation());
        if (slot < 0) {
            return InteractionResult.PASS;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof GunDisplayCaseBlockEntity displayCase)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ItemStack stored = displayCase.getItem(slot);
        if (stored.isEmpty()) {
            if (!held.isEmpty() && GunPegboardItemConfigRead.canPlace(held) && displayCase.setItem(slot, held)) {
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
                return InteractionResult.CONSUME;
            }
            return held.isEmpty() ? InteractionResult.CONSUME : InteractionResult.PASS;
        }

        if (displayCase.rotateItem(slot)) {
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(LOCKED)) {
            return switch (state.getValue(FACING)) {
                case SOUTH -> SOUTH_LOCKED_SHAPE;
                case WEST -> WEST_LOCKED_SHAPE;
                case EAST -> EAST_LOCKED_SHAPE;
                case NORTH -> NORTH_LOCKED_SHAPE;
                default -> Shapes.block();
            };
        }
        return switch (state.getValue(FACING)) {
            case SOUTH -> SOUTH_BACK_PANEL_SHAPE;
            case WEST -> WEST_BACK_PANEL_SHAPE;
            case EAST -> EAST_BACK_PANEL_SHAPE;
            case NORTH -> NORTH_BACK_PANEL_SHAPE;
            default -> Shapes.block();
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(LOCKED)) {
            return getShape(state, level, pos, context);
        }
        return switch (state.getValue(FACING)) {
            case SOUTH -> SOUTH_OPEN_SHAPE;
            case WEST -> WEST_OPEN_SHAPE;
            case EAST -> EAST_OPEN_SHAPE;
            case NORTH -> NORTH_OPEN_SHAPE;
            default -> Shapes.block();
        };
    }

    @Override
    public void onRemove(BlockState oldState, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!oldState.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof GunDisplayCaseBlockEntity displayCase) {
                displayCase.dropAllItems();
            }
        }
        super.onRemove(oldState, level, pos, newState, movedByPiston);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.DESTROY;
    }

    private BlockState updateConnections(LevelAccessor level, BlockPos pos, BlockState state, @Nullable Set<BlockPos> forceConnectedPositions) {
        Direction facing = state.getValue(FACING);
        return state
                .setValue(LEFT, shouldAutoConnectTo(level, pos, state, leftOf(facing), forceConnectedPositions))
                .setValue(RIGHT, shouldAutoConnectTo(level, pos, state, rightOf(facing), forceConnectedPositions))
                .setValue(UP, shouldAutoConnectTo(level, pos, state, Direction.UP, forceConnectedPositions))
                .setValue(DOWN, shouldAutoConnectTo(level, pos, state, Direction.DOWN, forceConnectedPositions));
    }

    private static Direction getPlacementFacing(BlockPlaceContext context, BlockState clickedState, boolean clickedDisplayCase) {
        if (clickedDisplayCase) {
            return clickedState.getValue(FACING);
        }
        Direction clickedFace = context.getClickedFace();
        return clickedFace.getAxis() == Direction.Axis.Y ? context.getHorizontalDirection().getOpposite() : clickedFace;
    }

    private static boolean shouldAutoConnectTo(LevelAccessor level, BlockPos pos, BlockState state, Direction direction, @Nullable Set<BlockPos> forceConnectedPositions) {
        BlockPos neighborPos = pos.relative(direction);
        BlockState neighborState = level.getBlockState(neighborPos);
        if (!connectsTo(state, neighborState)) {
            return false;
        }
        if (forceConnectedPositions != null && forceConnectedPositions.contains(neighborPos)) {
            return true;
        }
        BooleanProperty neighborProperty = propertyForDirection(neighborState.getValue(FACING), direction.getOpposite());
        return neighborProperty != null && neighborState.getValue(neighborProperty);
    }

    private static Set<BlockPos> collectConnectedCases(LevelAccessor level, BlockPos start, Direction facing) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> checked = new HashSet<>();
        Set<BlockPos> connected = new HashSet<>();
        queue.add(start);
        while (!queue.isEmpty() && checked.size() < MAX_GROUP_TOGGLE) {
            BlockPos pos = queue.removeFirst();
            if (!checked.add(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof GunDisplayCaseBlock) || state.getValue(FACING) != facing) {
                continue;
            }
            connected.add(pos);
            queueConnectedCases(queue, pos, state, facing);
        }
        return connected;
    }

    private static void syncConnectedNeighbors(Level level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        syncConnectedNeighbor(level, pos, state, leftOf(facing), LEFT);
        syncConnectedNeighbor(level, pos, state, rightOf(facing), RIGHT);
        syncConnectedNeighbor(level, pos, state, Direction.UP, UP);
        syncConnectedNeighbor(level, pos, state, Direction.DOWN, DOWN);
    }

    private static void syncConnectedNeighbor(Level level, BlockPos pos, BlockState state, Direction direction, BooleanProperty property) {
        if (!state.getValue(property)) {
            return;
        }
        BlockPos neighborPos = pos.relative(direction);
        BlockState neighborState = level.getBlockState(neighborPos);
        if (!connectsTo(state, neighborState)) {
            return;
        }
        BooleanProperty neighborProperty = propertyForDirection(neighborState.getValue(FACING), direction.getOpposite());
        if (neighborProperty != null && !neighborState.getValue(neighborProperty)) {
            level.setBlock(neighborPos, neighborState.setValue(neighborProperty, true), LOCK_UPDATE_FLAGS);
        }
    }

    private static boolean connectsTo(BlockState state, BlockState neighbor) {
        return neighbor.getBlock() instanceof GunDisplayCaseBlock && neighbor.getValue(FACING) == state.getValue(FACING);
    }

    private static BooleanProperty propertyForDirection(Direction facing, Direction direction) {
        if (direction == Direction.UP) {
            return UP;
        }
        if (direction == Direction.DOWN) {
            return DOWN;
        }
        if (direction == leftOf(facing)) {
            return LEFT;
        }
        if (direction == rightOf(facing)) {
            return RIGHT;
        }
        return null;
    }

    private static Direction leftOf(Direction facing) {
        return facing.getCounterClockWise();
    }

    private static Direction rightOf(Direction facing) {
        return facing.getClockWise();
    }

    public void toggleLockFromInteraction(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) {
            return;
        }
        toggleLinkedCases(level, pos, !state.getValue(LOCKED));
        level.playSound(null, pos, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 0.8F, state.getValue(LOCKED) ? 1.1F : 0.9F);
    }

    private void toggleLinkedCases(Level level, BlockPos start, boolean locked) {
        BlockState startState = level.getBlockState(start);
        if (!(startState.getBlock() instanceof GunDisplayCaseBlock)) {
            return;
        }
        Direction facing = startState.getValue(FACING);
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(start);
        while (!queue.isEmpty() && visited.size() < MAX_GROUP_TOGGLE) {
            BlockPos pos = queue.removeFirst();
            if (!visited.add(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof GunDisplayCaseBlock) || state.getValue(FACING) != facing) {
                continue;
            }
            if (state.getValue(LOCKED) != locked) {
                level.setBlock(pos, state.setValue(LOCKED, locked), LOCK_UPDATE_FLAGS);
            }
            queueConnectedCases(queue, pos, state, facing);
        }
    }

    private static void queueConnectedCases(ArrayDeque<BlockPos> queue, BlockPos pos, BlockState state, Direction facing) {
        if (state.getValue(LEFT)) {
            queue.add(pos.relative(leftOf(facing)));
        }
        if (state.getValue(RIGHT)) {
            queue.add(pos.relative(rightOf(facing)));
        }
        if (state.getValue(UP)) {
            queue.add(pos.above());
        }
        if (state.getValue(DOWN)) {
            queue.add(pos.below());
        }
    }
}
