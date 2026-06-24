package com.tacz.guns.block.entity;

import com.tacz.guns.block.AmmoStorageBlock;
import com.tacz.guns.init.ModBlocks;
import com.tacz.guns.util.AmmoStorageHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class AmmoStorageBlockEntity extends BlockEntity implements MenuProvider, Container {
    public static final BlockEntityType<AmmoStorageBlockEntity> TYPE = BlockEntityType.Builder.of(AmmoStorageBlockEntity::new,
            ModBlocks.AMMO_STORAGE.get(), ModBlocks.AMMO_STORAGE_GOLD.get(), ModBlocks.AMMO_STORAGE_DIAMOND.get()).build(null);
    public static final int STORAGE_SLOTS = 27;
    private static final String ENTRIES_TAG = "Entries";
    private static final String SLOT_TAG = "Slot";
    private static final String STACK_TAG = "Stack";
    private static final String COUNT_TAG = "Count";

    private final Entry[] entries = new Entry[STORAGE_SLOTS];
    private final LazyOptional<IItemHandler> itemHandler = LazyOptional.of(() -> new Handler());

    public AmmoStorageBlockEntity(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }

    public int insertAmmo(ItemStack stack, boolean simulate) {
        if (!AmmoStorageHelper.isValidAmmo(stack)) {
            return 0;
        }
        int remaining = stack.getCount();
        for (Entry entry : entries) {
            if (remaining <= 0) {
                break;
            }
            if (entry != null && ItemStack.isSameItemSameTags(entry.stack, stack)) {
                remaining -= insertIntoEntry(entry, remaining, simulate);
            }
        }
        for (int i = 0; i < entries.length && remaining > 0; i++) {
            if (entries[i] == null) {
                int inserted = Math.min(remaining, getSlotLimit(stack));
                if (!simulate && inserted > 0) {
                    ItemStack sample = stack.copy();
                    sample.setCount(1);
                    entries[i] = new Entry(sample, inserted);
                    update();
                }
                remaining -= inserted;
            }
        }
        int inserted = stack.getCount() - remaining;
        if (!simulate && inserted > 0 && normalizeEntries()) {
            update();
        }
        return inserted;
    }

    public int insertAmmoIntoSlot(int slot, ItemStack stack, boolean simulate) {
        if (slot < 0 || slot >= entries.length || !AmmoStorageHelper.isValidAmmo(stack)) {
            return 0;
        }
        Entry entry = entries[slot];
        if (entry == null) {
            return insertAmmo(stack, simulate);
        }
        if (!ItemStack.isSameItemSameTags(entry.stack, stack)) {
            return 0;
        }
        return insertAmmo(stack, simulate);
    }

    public ItemStack extractAmmo(ResourceLocation ammoId, int count, boolean simulate) {
        ItemStack result = ItemStack.EMPTY;
        int remaining = count;
        for (int i = 0; i < entries.length && remaining > 0; i++) {
            Entry entry = entries[i];
            if (entry != null && ammoId.equals(AmmoStorageHelper.getAmmoId(entry.stack))) {
                ItemStack extracted = extractFromSlot(i, remaining, simulate, false);
                if (!extracted.isEmpty()) {
                    if (result.isEmpty()) {
                        result = extracted;
                    } else {
                        result.grow(extracted.getCount());
                    }
                    remaining -= extracted.getCount();
                }
            }
        }
        if (!simulate && !result.isEmpty()) {
            normalizeEntries();
            update();
        }
        return result;
    }

    public ItemStack extractOneStackFromSlot(int slot, boolean simulate) {
        Entry entry = getEntry(slot);
        if (entry == null) {
            return ItemStack.EMPTY;
        }
        return extractFromSlot(slot, AmmoStorageHelper.getStackSize(entry.stack), simulate);
    }

    public int getStoredCount(ResourceLocation ammoId) {
        int count = 0;
        for (Entry entry : entries) {
            if (entry != null && ammoId.equals(AmmoStorageHelper.getAmmoId(entry.stack))) {
                count += entry.count;
            }
        }
        return count;
    }

    public int getStoredCount(int slot) {
        Entry entry = getEntry(slot);
        return entry == null ? 0 : entry.count;
    }

    public ItemStack getSampleStack(int slot) {
        Entry entry = getEntry(slot);
        return entry == null ? ItemStack.EMPTY : entry.stack.copy();
    }

    public int getComparatorSignal() {
        if (isEmpty()) {
            return 0;
        }
        double fullness = 0.0D;
        for (Entry entry : entries) {
            if (entry != null) {
                fullness += Math.min(1.0D, (double) entry.count / getSlotLimit(entry.stack));
            }
        }
        return Math.max(1, Math.min(15, (int) Math.floor(14.0D * fullness / entries.length) + 1));
    }

    @Override
    public int getContainerSize() {
        return STORAGE_SLOTS;
    }

    @Override
    public boolean isEmpty() {
        for (Entry entry : entries) {
            if (entry != null && entry.count > 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        Entry entry = getEntry(slot);
        if (entry == null) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = entry.stack.copy();
        stack.setCount(Math.min(entry.count, AmmoStorageHelper.getStackSize(stack)));
        return stack;
    }

    public ItemStack getExternalItem(int slot) {
        Entry entry = getEntry(slot);
        if (entry == null) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = entry.stack.copy();
        stack.setCount(entry.count);
        return stack;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return extractFromSlot(slot, amount, false);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        Entry entry = getEntry(slot);
        if (entry == null) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = entry.stack.copy();
        stack.setCount(entry.count);
        entries[slot] = null;
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= entries.length) {
            return;
        }
        Entry existing = entries[slot];
        if (level != null && level.isClientSide() && existing != null && ItemStack.isSameItemSameTags(existing.stack, stack) && existing.count >= stack.getCount()) {
            return;
        }
        if (stack.isEmpty()) {
            entries[slot] = null;
            normalizeEntries();
            update();
            return;
        }
        if (!AmmoStorageHelper.isValidAmmo(stack)) {
            return;
        }
        ItemStack sample = stack.copy();
        sample.setCount(1);
        entries[slot] = new Entry(sample, Math.min(stack.getCount(), getSlotLimit(sample)));
        normalizeEntries();
        update();
    }

    @Override
    public void setChanged() {
        super.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return AmmoStorageHelper.isValidAmmo(stack);
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < entries.length; i++) {
            entries[i] = null;
        }
        update();
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        for (int i = 0; i < entries.length; i++) {
            entries[i] = null;
        }
        if (tag.contains(ENTRIES_TAG, Tag.TAG_LIST)) {
            ListTag list = tag.getList(ENTRIES_TAG, Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entryTag = list.getCompound(i);
                int slot = entryTag.getByte(SLOT_TAG) & 255;
                if (slot < 0 || slot >= entries.length) {
                    continue;
                }
                ItemStack stack = ItemStack.of(entryTag.getCompound(STACK_TAG));
                int count = entryTag.getInt(COUNT_TAG);
                if (AmmoStorageHelper.isValidAmmo(stack) && count > 0) {
                    stack.setCount(count);
                    insertAmmo(stack, false);
                }
            }
        }
        normalizeEntries();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ListTag list = new ListTag();
        for (int i = 0; i < entries.length; i++) {
            Entry entry = entries[i];
            if (entry == null || entry.count <= 0) {
                continue;
            }
            CompoundTag entryTag = new CompoundTag();
            entryTag.putByte(SLOT_TAG, (byte) i);
            entryTag.put(STACK_TAG, entry.stack.save(new CompoundTag()));
            entryTag.putInt(COUNT_TAG, entry.count);
            list.add(entryTag);
        }
        tag.put(ENTRIES_TAG, list);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("gui.tacz.ammo_storage.title");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new com.tacz.guns.inventory.AmmoStorageMenu(id, inventory, worldPosition);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandler.invalidate();
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    private int insertIntoEntry(Entry entry, int amount, boolean simulate) {
        int inserted = Math.min(amount, Math.max(0, getSlotLimit(entry.stack) - entry.count));
        if (!simulate && inserted > 0) {
            entry.count += inserted;
            update();
        }
        return inserted;
    }

    private ItemStack extractFromSlot(int slot, int amount, boolean simulate) {
        return extractFromSlot(slot, amount, simulate, true);
    }

    private ItemStack extractFromSlot(int slot, int amount, boolean simulate, boolean normalize) {
        Entry entry = getEntry(slot);
        if (entry == null || amount <= 0) {
            return ItemStack.EMPTY;
        }
        int extracted = Math.min(amount, entry.count);
        ItemStack result = entry.stack.copy();
        result.setCount(extracted);
        if (!simulate) {
            entry.count -= extracted;
            if (entry.count <= 0) {
                entries[slot] = null;
            }
            if (normalize) {
                normalizeEntries();
            }
            update();
        }
        return result;
    }

    private boolean normalizeEntries() {
        boolean changed = compactEntries();
        return sortEntries() || changed;
    }

    private boolean sortEntries() {
        Entry[] previous = entries.clone();
        Arrays.sort(entries, AmmoStorageBlockEntity::compareEntries);
        return !Arrays.equals(previous, entries);
    }

    private static int compareEntries(@Nullable Entry left, @Nullable Entry right) {
        if (left == right) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        int sort = Integer.compare(AmmoStorageHelper.getSort(left.stack), AmmoStorageHelper.getSort(right.stack));
        if (sort != 0) {
            return sort;
        }
        ResourceLocation leftId = AmmoStorageHelper.getAmmoId(left.stack);
        ResourceLocation rightId = AmmoStorageHelper.getAmmoId(right.stack);
        int id = String.valueOf(leftId).compareTo(String.valueOf(rightId));
        if (id != 0) {
            return id;
        }
        return String.valueOf(left.stack.getTag()).compareTo(String.valueOf(right.stack.getTag()));
    }

    private boolean compactEntries() {
        boolean changed = false;
        for (int i = 0; i < entries.length; i++) {
            Entry target = entries[i];
            if (target == null || target.count <= 0) {
                if (target != null) {
                    changed = true;
                }
                entries[i] = null;
                continue;
            }
            for (int j = i + 1; j < entries.length; j++) {
                Entry source = entries[j];
                if (source == null || source.count <= 0) {
                    if (source != null) {
                        entries[j] = null;
                        changed = true;
                    }
                    continue;
                }
                if (!ItemStack.isSameItemSameTags(target.stack, source.stack)) {
                    continue;
                }
                int moved = Math.min(Math.max(0, getSlotLimit(target.stack) - target.count), source.count);
                if (moved > 0) {
                    target.count += moved;
                    source.count -= moved;
                    changed = true;
                }
                if (source.count <= 0) {
                    entries[j] = null;
                    changed = true;
                }
            }
        }
        return changed;
    }

    @Nullable
    private Entry getEntry(int slot) {
        if (slot < 0 || slot >= entries.length) {
            return null;
        }
        return entries[slot];
    }

    private void update() {
        setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
            level.updateNeighbourForOutputSignal(worldPosition, state.getBlock());
        }
    }

    private int getSlotLimit(ItemStack stack) {
        return AmmoStorageHelper.getStorageSlotLimit(stack, getSlotStacks());
    }

    public int getSlotStacks() {
        Block block = getBlockState().getBlock();
        if (block instanceof AmmoStorageBlock ammoStorageBlock) {
            return ammoStorageBlock.getSlotStacks();
        }
        return AmmoStorageBlock.IRON_SLOT_STACKS;
    }

    private static class Entry {
        private final ItemStack stack;
        private int count;

        private Entry(ItemStack stack, int count) {
            this.stack = stack;
            this.count = count;
        }
    }

    private class Handler implements IItemHandler {
        @Override
        public int getSlots() {
            return STORAGE_SLOTS;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return getExternalItem(slot);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            int inserted = insertAmmoIntoSlot(slot, stack, simulate);
            ItemStack remaining = stack.copy();
            remaining.shrink(inserted);
            return remaining;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            Entry entry = getEntry(slot);
            if (entry == null) {
                return ItemStack.EMPTY;
            }
            return extractFromSlot(slot, Math.min(amount, AmmoStorageHelper.getStackSize(entry.stack)), simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            Entry entry = getEntry(slot);
            return entry == null ? Integer.MAX_VALUE : AmmoStorageBlockEntity.this.getSlotLimit(entry.stack);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return AmmoStorageHelper.isValidAmmo(stack);
        }
    }
}
