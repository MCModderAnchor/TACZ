package com.tacz.guns.block.entity;

import com.tacz.guns.block.AmmoStorageBlock;
import com.tacz.guns.init.ModBlocks;
import com.tacz.guns.inventory.AmmoStorageInventory;
import com.tacz.guns.inventory.AmmoStorageMenu;
import com.tacz.guns.util.AmmoStorageHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AmmoStorageBlockEntity extends BlockEntity implements MenuProvider {
    public static final BlockEntityType<AmmoStorageBlockEntity> TYPE = BlockEntityType.Builder.of(AmmoStorageBlockEntity::new,
            ModBlocks.AMMO_STORAGE.get(), ModBlocks.AMMO_STORAGE_GOLD.get(), ModBlocks.AMMO_STORAGE_DIAMOND.get()).build(null);
    public static final int MAX_AMMO_TYPES = 27;

    private final AmmoStorageInventory inventory = new AmmoStorageInventory(MAX_AMMO_TYPES, this::getCapacityStacks,
            AmmoStorageHelper::isValidAmmo, AmmoStorageHelper::getStackSize, this::onInventoryChanged);
    private LazyOptional<IItemHandler> itemHandler = LazyOptional.of(() -> inventory);

    public AmmoStorageBlockEntity(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }

    public int insertAmmo(ItemStack stack, boolean simulate) {
        return inventory.insert(stack, simulate);
    }

    public ItemStack extractAmmo(ItemStack sample, int count, boolean simulate) {
        return inventory.extract(sample, count, simulate);
    }

    public ItemStack extractAmmo(ResourceLocation ammoId, int count, boolean simulate) {
        for (AmmoStorageInventory.Content content : inventory.getContents()) {
            if (ammoId.equals(AmmoStorageHelper.getAmmoId(content.sample()))) {
                // Never combine differently tagged ammo into one returned ItemStack.
                return inventory.extract(content.sample(), count, simulate);
            }
        }
        return ItemStack.EMPTY;
    }

    public int moveAmmoToInventory(Container target, int targetSlots, ItemStack sample, int count) {
        return inventory.moveToInventory(target, targetSlots, sample, count);
    }

    public List<AmmoStorageInventory.Content> getContents() {
        return inventory.getContents();
    }

    public long getInventoryRevision() {
        return inventory.getRevision();
    }

    public long getUsedStacks() {
        return inventory.getUsedStacks();
    }

    public long getRemainingCapacity(ItemStack sample) {
        return inventory.getRemainingCapacity(sample);
    }

    public int getComparatorSignal() {
        return inventory.getComparatorSignal();
    }

    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 64;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inventory.load(tag.getList("Entries", Tag.TAG_COMPOUND));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Entries", inventory.save());
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("gui.tacz.ammo_storage.title");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new AmmoStorageMenu(id, inventory, this);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandler.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        itemHandler = LazyOptional.of(() -> inventory);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (!isRemoved() && cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    private void onInventoryChanged() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
        // Contents are sent only to open menus, never through block-entity/chunk synchronization.
    }

    public int getCapacityStacks() {
        return getBlockState().getBlock() instanceof AmmoStorageBlock storage
                ? storage.getCapacityStacks() : AmmoStorageBlock.IRON_CAPACITY_STACKS;
    }
}
