package com.tacz.guns.inventory;

import com.tacz.guns.block.entity.AmmoStorageBlockEntity;
import com.tacz.guns.init.ModContainer;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ServerMessageAmmoStorageContents;
import com.tacz.guns.util.AmmoStorageHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AmmoStorageMenu extends AbstractContainerMenu {
    public static final int STORAGE_ROWS = 3;
    public static final int STORAGE_COLUMNS = 9;
    public static final int STORAGE_SLOT_COUNT = STORAGE_ROWS * STORAGE_COLUMNS;
    public static final int STATUS_HEIGHT = 26;
    public static final int STORAGE_Y = 18 + STATUS_HEIGHT;
    public static final int INVENTORY_X = 8;
    public static final int INVENTORY_Y = 85 + STATUS_HEIGHT;
    public static final int HOTBAR_Y = 143 + STATUS_HEIGHT;
    private final Inventory playerInventory;
    @Nullable
    private final AmmoStorageBlockEntity storage;
    private List<AmmoStorageEntry> entries = List.of();
    private long sentRevision = -1;
    private int capacityStacks;
    private long usedStacks;
    private int nextEntryId;

    /** Client menu: neither rendering nor vanilla slot sync requires a local block entity. */
    public AmmoStorageMenu(int id, Inventory inventory) {
        this(id, inventory, null);
    }

    public AmmoStorageMenu(int id, Inventory inventory, @Nullable AmmoStorageBlockEntity storage) {
        super(ModContainer.AMMO_STORAGE_MENU.get(), id);
        this.playerInventory = inventory;
        this.storage = storage;
        // Only real player slots belong to this menu. Sorters cannot reach bulk storage.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, INVENTORY_X + col * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, INVENTORY_X + col * 18, HOTBAR_Y));
        }
    }

    public List<AmmoStorageEntry> getEntries() {
        return entries;
    }

    public int getCapacityStacks() {
        return capacityStacks;
    }

    public long getUsedStacks() {
        return usedStacks;
    }

    public void setContents(int capacityStacks, long usedStacks, List<AmmoStorageEntry> entries) {
        if (playerInventory.player.level().isClientSide()) {
            this.capacityStacks = capacityStacks;
            this.usedStacks = usedStacks;
            this.entries = List.copyOf(entries);
        }
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (storage == null) {
            return;
        }
        int capacity = storage.getCapacityStacks();
        long used = storage.getUsedStacks();
        if (storage.getInventoryRevision() == sentRevision && capacity == capacityStacks && used == usedStacks) {
            // Stack sizes may change during a resource reload without changing item counts.
            boolean remainingChanged = entries.stream().anyMatch(entry ->
                    entry.remainingCapacity() != storage.getRemainingCapacity(entry.sample()));
            if (!remainingChanged) {
                return;
            }
        }
        List<AmmoStorageEntry> updated = new ArrayList<>();
        for (AmmoStorageInventory.Content content : storage.getContents()) {
            int id = -1;
            for (AmmoStorageEntry previous : entries) {
                if (ItemStack.isSameItemSameComponents(previous.sample(), content.sample())) {
                    id = previous.id();
                    break;
                }
            }
            // Never reuse a removed entry's ID: delayed clicks cannot target new ammo.
            updated.add(new AmmoStorageEntry(id >= 0 ? id : nextEntryId++, content.sample(), content.count(),
                    storage.getRemainingCapacity(content.sample())));
        }
        updated.sort(Comparator.comparingInt((AmmoStorageEntry entry) -> AmmoStorageHelper.getSort(entry.sample()))
                .thenComparing(entry -> String.valueOf(AmmoStorageHelper.getAmmoId(entry.sample())))
                .thenComparing(entry -> String.valueOf(entry.sample().getComponents())));
        entries = List.copyOf(updated);
        capacityStacks = capacity;
        usedStacks = used;
        sentRevision = storage.getInventoryRevision();
        NetworkHandler.sendToClientPlayer(new ServerMessageAmmoStorageContents(containerId, capacityStacks, usedStacks, entries), playerInventory.player);
    }

    public void handleInteraction(Player player, int entryId, Action action) {
        if (storage == null || player.containerMenu != this || player.isSpectator() || !player.isAlive() || !stillValid(player)) {
            return;
        }
        AmmoStorageEntry entry = entries.stream().filter(value -> value.id() == entryId).findFirst().orElse(null);
        if (action == Action.QUICK_MOVE) {
            if (entry != null) {
                storage.moveAmmoToInventory(playerInventory, 36, entry.sample(), getTransferStackSize(entry));
            }
        } else if (!getCarried().isEmpty()) {
            ItemStack held = getCarried();
            int requested = action == Action.PICKUP_HALF ? 1 : held.getCount();
            int inserted = storage.insertAmmo(held.copyWithCount(requested), false);
            if (inserted > 0) {
                setCarried(held.copyWithCount(held.getCount() - inserted));
            }
        } else if (entry != null) {
            int amount = storage.extractAmmo(entry.sample(), getTransferStackSize(entry), true).getCount();
            if (action == Action.PICKUP_HALF) {
                amount = amount / 2 + amount % 2;
            }
            setCarried(storage.extractAmmo(entry.sample(), amount, false));
        }
        broadcastChanges();
    }

    private int getTransferStackSize(AmmoStorageEntry entry) {
        return Math.min(playerInventory.getMaxStackSize(), entry.sample().getMaxStackSize());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (storage == null || index < 0 || index >= slots.size() || player.isSpectator() || !stillValid(player)) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        ItemStack stack = slot.getItem();
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack original = stack.copy();
        if (AmmoStorageHelper.isValidAmmo(stack)) {
            int inserted = storage.insertAmmo(stack, false);
            if (inserted == 0) {
                return ItemStack.EMPTY;
            }
            stack.shrink(inserted);
        } else if (!moveItemStackTo(stack, index < 27 ? 27 : 0, index < 27 ? 36 : 27, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, stack);
        broadcastChanges();
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().isClientSide() || storage != null && storage.stillValid(player);
    }

    public enum Action {
        PICKUP,
        PICKUP_HALF,
        QUICK_MOVE
    }
}
