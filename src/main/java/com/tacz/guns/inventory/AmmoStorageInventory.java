package com.tacz.guns.inventory;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

// 弹药库的特殊库存。不作为标准的容器提供，避免被按格意外操作，仍可被作为IItemHandler使用
public class AmmoStorageInventory implements IItemHandler {
    private final Entry[] entries;
    private final IntSupplier capacityStacks;
    private final Predicate<ItemStack> validator;
    private final ToIntFunction<ItemStack> stackSize;
    private final Runnable onChanged;
    private long revision;

    public AmmoStorageInventory(int types, IntSupplier capacityStacks, Predicate<ItemStack> validator,
                                ToIntFunction<ItemStack> stackSize, Runnable onChanged) {
        this.entries = new Entry[types];
        this.capacityStacks = capacityStacks;
        this.validator = validator;
        this.stackSize = stackSize;
        this.onChanged = onChanged;
    }

    public long getRevision() {
        return revision;
    }

    public int getCapacityStacks() {
        return Math.max(0, capacityStacks.getAsInt());
    }

    public long getUsedStacks() {
        long used = 0;
        for (Entry entry : entries) {
            if (entry != null) {
                used = saturatedAdd(used, stacksFor(entry.count, entry.sample));
            }
        }
        return used;
    }

    public int getUsedTypes() {
        int used = 0;
        for (Entry entry : entries) {
            if (entry != null) {
                used++;
            }
        }
        return used;
    }

    /** Includes unused space in this variant's partially filled final stack. */
    public long getRemainingCapacity(ItemStack sample) {
        if (sample.isEmpty() || !validator.test(sample)) {
            return 0;
        }
        int slot = findMatchingSlot(sample);
        if (slot < 0 && findEmptySlot() < 0) {
            return 0;
        }
        long used = getUsedStacks();
        int capacity = getCapacityStacks();
        if (used > capacity) {
            // A data reload may reduce capacity or stack sizes. Keep stock, but block insertion.
            return 0;
        }
        long count = slot < 0 ? 0 : entries[slot].count;
        long availableStacks = capacity - used + stacksFor(count, sample);
        return Math.max(0, availableStacks * getStackSize(sample) - count);
    }

    public int insert(ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || !validator.test(stack)) {
            return 0;
        }
        int slot = findMatchingSlot(stack);
        if (slot < 0) {
            slot = findEmptySlot();
        }
        int inserted = slot < 0 ? 0 : (int) insertIntoSlot(slot, stack, stack.getCount(), simulate);
        if (!simulate && inserted > 0) {
            changed();
        }
        return inserted;
    }

    @Override
    public int getSlots() {
        return entries.length;
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        Entry entry = getEntry(slot);
        // ItemStack can only express an int. Bulk contents and saves retain the full long count.
        return entry == null ? ItemStack.EMPTY : entry.sample.copyWithCount((int) Math.min(Integer.MAX_VALUE, entry.count));
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (!isItemValid(slot, stack)) {
            return stack;
        }
        int inserted = (int) insertIntoSlot(slot, stack, stack.getCount(), simulate);
        if (!simulate && inserted > 0) {
            changed();
        }
        return inserted == stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - inserted);
    }

    private long insertIntoSlot(int slot, ItemStack sample, long amount, boolean simulate) {
        Entry entry = entries[slot];
        int matching = findMatchingSlot(sample);
        if (amount <= 0 || (entry != null && !ItemStack.isSameItemSameTags(entry.sample, sample))
                || (matching >= 0 && matching != slot)) {
            return 0;
        }
        long inserted = Math.min(amount, getRemainingCapacity(sample));
        if (!simulate && inserted > 0) {
            if (entry == null) {
                entries[slot] = new Entry(sample.copyWithCount(1), inserted);
            } else {
                entry.count += inserted;
            }
        }
        return inserted;
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        Entry entry = getEntry(slot);
        if (entry == null || amount <= 0) {
            return ItemStack.EMPTY;
        }
        return extractFromSlot(slot, Math.min(amount, Math.max(0, entry.sample.getMaxStackSize())), simulate);
    }

    /** Bulk API; unlike IItemHandler extraction, this may return more than one stack. */
    public ItemStack extract(ItemStack sample, int amount, boolean simulate) {
        int slot = sample.isEmpty() ? -1 : findMatchingSlot(sample);
        return slot < 0 ? ItemStack.EMPTY : extractFromSlot(slot, amount, simulate);
    }

    private ItemStack extractFromSlot(int slot, int amount, boolean simulate) {
        Entry entry = entries[slot];
        int extracted = (int) Math.min(Math.max(0, amount), entry.count);
        if (extracted == 0) {
            return ItemStack.EMPTY;
        }
        ItemStack result = entry.sample.copyWithCount(extracted);
        if (!simulate) {
            entry.count -= extracted;
            if (entry.count == 0) {
                entries[slot] = null;
            }
            changed();
        }
        return result;
    }

    /** Moves only what fits, on the server thread, without rolling back by slot index. */
    public int moveToInventory(Container target, int targetSlots, ItemStack sample, int amount) {
        if (sample.isEmpty() || amount <= 0) {
            return 0;
        }
        int remaining = amount;
        int end = Math.min(targetSlots, target.getContainerSize());
        for (int pass = 0; pass < 2 && remaining > 0; pass++) {
            for (int slot = end - 1; slot >= 0 && remaining > 0; slot--) {
                ItemStack existing = target.getItem(slot);
                if ((existing.isEmpty() != (pass == 1)) || !target.canPlaceItem(slot, sample)
                        || (!existing.isEmpty() && !ItemStack.isSameItemSameTags(existing, sample))) {
                    continue;
                }
                int limit = Math.min(target.getMaxStackSize(), sample.getMaxStackSize());
                int accepted = Math.min(remaining, Math.max(0, limit - existing.getCount()));
                ItemStack extracted = extract(sample, accepted, false);
                if (!extracted.isEmpty()) {
                    target.setItem(slot, extracted.copyWithCount(existing.getCount() + extracted.getCount()));
                    remaining -= extracted.getCount();
                }
            }
        }
        int moved = amount - remaining;
        if (moved > 0) {
            target.setChanged();
        }
        return moved;
    }

    @Override
    public int getSlotLimit(int slot) {
        if (slot < 0 || slot >= entries.length) {
            return 0;
        }
        Entry entry = entries[slot];
        return entry == null ? Integer.MAX_VALUE
                : (int) Math.min(Integer.MAX_VALUE, (long) getCapacityStacks() * getStackSize(entry.sample));
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return slot >= 0 && slot < entries.length && !stack.isEmpty() && validator.test(stack);
    }

    public List<Content> getContents() {
        List<Content> result = new ArrayList<>();
        for (Entry entry : entries) {
            if (entry != null) {
                result.add(new Content(entry.sample.copy(), entry.count));
            }
        }
        return List.copyOf(result);
    }

    public int getComparatorSignal() {
        long used = getUsedStacks();
        int capacity = Math.max(1, getCapacityStacks());
        return used == 0 ? 0 : (int) (14L * Math.min(used, capacity) / capacity + 1);
    }

    public ListTag save() {
        ListTag result = new ListTag();
        for (int slot = 0; slot < entries.length; slot++) {
            Entry entry = entries[slot];
            if (entry != null) {
                CompoundTag tag = new CompoundTag();
                tag.putByte("Slot", (byte) slot);
                tag.put("Stack", entry.sample.save(new CompoundTag()));
                tag.putLong("Count", entry.count);
                result.add(tag);
            }
        }
        return result;
    }

    /** Restores saved stock, including excess capacity, without invoking world updates. */
    public void load(ListTag list) {
        Arrays.fill(entries, null);
        BitSet seenSlots = new BitSet(entries.length);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            int oldSlot = tag.getInt("Slot");
            if (!tag.contains("Slot", Tag.TAG_ANY_NUMERIC) || oldSlot < 0 || oldSlot >= entries.length
                    || seenSlots.get(oldSlot) || !tag.contains("Stack", Tag.TAG_COMPOUND)) {
                continue;
            }
            try {
                ItemStack sample = ItemStack.of(tag.getCompound("Stack"));
                long count = tag.getLong("Count");
                if (sample.isEmpty() || count <= 0 || !validator.test(sample)) {
                    continue;
                }
                seenSlots.set(oldSlot);
                int slot = findMatchingSlot(sample);
                if (slot < 0) {
                    slot = entries[oldSlot] == null ? oldSlot : findEmptySlot();
                }
                if (slot >= 0) {
                    // Capacity limits apply to new insertions, never to existing saved items.
                    Entry entry = entries[slot];
                    if (entry == null) {
                        entries[slot] = new Entry(sample.copyWithCount(1), count);
                    } else {
                        entry.count = saturatedAdd(entry.count, count);
                    }
                }
            } catch (RuntimeException ignored) {
                // Malformed legacy items/capabilities must not prevent the world from loading.
            }
        }
        revision++;
    }

    private int getStackSize(ItemStack sample) {
        return Math.max(1, stackSize.applyAsInt(sample));
    }

    private long stacksFor(long count, ItemStack sample) {
        return count == 0 ? 0 : 1 + (count - 1) / getStackSize(sample);
    }

    private static long saturatedAdd(long left, long right) {
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }

    private int findMatchingSlot(ItemStack sample) {
        for (int slot = 0; slot < entries.length; slot++) {
            if (entries[slot] != null && ItemStack.isSameItemSameTags(entries[slot].sample, sample)) {
                return slot;
            }
        }
        return -1;
    }

    private int findEmptySlot() {
        for (int slot = 0; slot < entries.length; slot++) {
            if (entries[slot] == null) {
                return slot;
            }
        }
        return -1;
    }

    private Entry getEntry(int slot) {
        return slot < 0 || slot >= entries.length ? null : entries[slot];
    }

    private void changed() {
        revision++;
        onChanged.run();
    }

    public record Content(ItemStack sample, long count) {
    }

    private static final class Entry {
        private final ItemStack sample;
        private long count;

        private Entry(ItemStack sample, long count) {
            this.sample = sample;
            this.count = count;
        }
    }
}
