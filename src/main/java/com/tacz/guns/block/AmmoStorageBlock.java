package com.tacz.guns.block;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.block.entity.AmmoStorageBlockEntity;
import com.tacz.guns.init.ModBlocks;
import com.tacz.guns.util.AmmoStorageHelper;
import com.tacz.guns.util.AmmoStorageInteractionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class AmmoStorageBlock extends BaseEntityBlock {
    public static final com.mojang.serialization.MapCodec<AmmoStorageBlock> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.mapCodec(instance -> instance.group(
            com.mojang.serialization.Codec.INT.fieldOf("capacityStacks").forGetter((AmmoStorageBlock block) -> block.capacityStacks),
            propertiesCodec()).apply(instance, AmmoStorageBlock::new));
    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final int IRON_CAPACITY_STACKS = 216;
    public static final int GOLD_CAPACITY_STACKS = 432;
    public static final int DIAMOND_CAPACITY_STACKS = 864;

    private final int capacityStacks;

    public AmmoStorageBlock() {
        this(IRON_CAPACITY_STACKS);
    }

    public AmmoStorageBlock(int capacityStacks) {
        this(capacityStacks, Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL).strength(2.0F).dynamicShape());
    }

    public AmmoStorageBlock(int capacityStacks, Properties properties) {
        super(properties);
        this.capacityStacks = capacityStacks;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public int getCapacityStacks() {
        return capacityStacks;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AmmoStorageBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
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
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof AmmoStorageBlockEntity storage)) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof IAmmoBox ammoBox) {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            if (player.isShiftKeyDown()) {
                return AmmoStorageInteractionHelper.unloadAmmoBox(storage, held, ammoBox, player);
            }
            return fillAmmoBox(storage, held, ammoBox, player);
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(storage);
        }
        return InteractionResult.CONSUME;
    }

    private InteractionResult fillAmmoBox(AmmoStorageBlockEntity storage, ItemStack boxStack, IAmmoBox ammoBox, Player player) {
        ResourceLocation ammoId = ammoBox.getAmmoId(boxStack);
        if (ammoId.equals(DefaultAssets.EMPTY_AMMO_ID)) {
            player.displayClientMessage(Component.translatable("message.tacz.ammo_storage.ammo_box_empty"), true);
            return InteractionResult.CONSUME;
        }
        int current = ammoBox.getAmmoCount(boxStack);
        int levelMultiplier = ammoBox.getAmmoLevel(boxStack) + 1;
        int max = AmmoStorageHelper.getStackSize(AmmoStorageHelper.createAmmoStack(ammoId, 1)) * com.tacz.guns.config.sync.SyncConfig.AMMO_BOX_STACK_SIZE.get() * levelMultiplier;
        int need = Math.max(0, max - current);
        if (need <= 0) {
            return InteractionResult.CONSUME;
        }
        ItemStack extracted = storage.extractAmmo(ammoId, need, false);
        if (extracted.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.tacz.ammo_storage.no_ammo_box_stock"), true);
            return InteractionResult.CONSUME;
        }
        ammoBox.setAmmoCount(boxStack, current + extracted.getCount());
        player.displayClientMessage(Component.translatable("message.tacz.ammo_storage.picked", extracted.getHoverName(), extracted.getCount()), true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.PLAYERS, 0.8F, 1.0F);
        return InteractionResult.CONSUME;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        BlockEntity blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        ItemStack stack = new ItemStack(state.getBlock());
        if (blockEntity instanceof AmmoStorageBlockEntity storage) {
            CompoundTag tag = storage.saveWithoutMetadata(builder.getLevel().registryAccess());
            if (!tag.isEmpty()) {
                BlockItem.setBlockEntityData(stack, ModBlocks.AMMO_STORAGE_BE.get(), tag);
            }
        }
        return Collections.singletonList(stack);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof AmmoStorageBlockEntity storage) {
            return storage.getComparatorSignal();
        }
        return 0;
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
