package com.tacz.guns.block;

import com.tacz.guns.block.entity.GunPegboardBlockEntity;
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
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class GunPegboardBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape NORTH_SHAPE = Block.box(0, 0, 13, 16, 16, 16);
    private static final VoxelShape SOUTH_SHAPE = Block.box(0, 0, 0, 16, 16, 3);
    private static final VoxelShape WEST_SHAPE = Block.box(13, 0, 0, 16, 16, 16);
    private static final VoxelShape EAST_SHAPE = Block.box(0, 0, 0, 3, 16, 16);
    private final boolean glowing;

    public GunPegboardBlock() {
        this(false);
    }

    public GunPegboardBlock(boolean glowing) {
        super(Properties.of().mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(2.0F, 3.0F).noOcclusion().lightLevel(state -> glowing ? 10 : 0));
        this.glowing = glowing;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public boolean isGlowing() {
        return glowing;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GunPegboardBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        if (clickedFace.getAxis() == Direction.Axis.Y) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        }
        return defaultBlockState().setValue(FACING, clickedFace);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
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
        return switch (state.getValue(FACING)) {
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
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.DESTROY;
    }
}
