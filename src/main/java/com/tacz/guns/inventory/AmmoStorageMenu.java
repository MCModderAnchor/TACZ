package com.tacz.guns.inventory;

import com.tacz.guns.block.entity.AmmoStorageBlockEntity;
import com.tacz.guns.init.ModContainer;
import com.tacz.guns.util.AmmoStorageHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class AmmoStorageMenu extends AbstractContainerMenu {
    public static final int STORAGE_ROWS = 3;
    public static final int STORAGE_COLUMNS = 9;
    public static final int STORAGE_SLOT_COUNT = STORAGE_ROWS * STORAGE_COLUMNS;
    public static final int INVENTORY_X = 8;
    public static final int INVENTORY_Y = 85;
    public static final int HOTBAR_Y = 143;
    private final Inventory playerInventory;
    private final BlockPos pos;
    private final AmmoStorageBlockEntity storage;

    public AmmoStorageMenu(int id, Inventory inventory, BlockPos pos) {
        super(ModContainer.AMMO_STORAGE_MENU.get(), id);
        this.playerInventory = inventory;
        this.pos = pos;
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        this.storage = blockEntity instanceof AmmoStorageBlockEntity ammoStorage ? ammoStorage : null;
        Container storageContainer = storage == null ? new net.minecraft.world.SimpleContainer(STORAGE_SLOT_COUNT) : storage;

        for (int row = 0; row < STORAGE_ROWS; ++row) {
            for (int col = 0; col < STORAGE_COLUMNS; ++col) {
                int slot = col + row * STORAGE_COLUMNS;
                addSlot(new AmmoSlot(storageContainer, storage, inventory.player.level().isClientSide(), slot, INVENTORY_X + col * 18, 18 + row * 18));
            }
        }
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                addSlot(new Slot(inventory, col + row * 9 + 9, INVENTORY_X + col * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            addSlot(new Slot(inventory, col, INVENTORY_X + col * 18, HOTBAR_Y));
        }
    }

    public BlockPos getPos() {
        return pos;
    }

    public AmmoStorageBlockEntity getStorage() {
        return storage;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < STORAGE_SLOT_COUNT) {
            if (storage == null) {
                return ItemStack.EMPTY;
            }
            ItemStack extracted = storage.extractOneStackFromSlot(index, false);
            if (extracted.isEmpty()) {
                return ItemStack.EMPTY;
            }
            ItemStack moved = extracted.copy();
            if (!moveItemStackTo(moved, STORAGE_SLOT_COUNT, slots.size(), true)) {
                storage.insertAmmoIntoSlot(index, extracted, false);
                return ItemStack.EMPTY;
            }
            if (!moved.isEmpty()) {
                storage.insertAmmoIntoSlot(index, moved, false);
            }
            broadcastChanges();
            return ItemStack.EMPTY;
        } else if (storage != null && AmmoStorageHelper.isValidAmmo(stack)) {
            int inserted = storage.insertAmmo(stack, false);
            if (inserted <= 0) {
                return ItemStack.EMPTY;
            }
            stack.shrink(inserted);
        } else if (index < STORAGE_SLOT_COUNT + 27) {
            if (!moveItemStackTo(stack, STORAGE_SLOT_COUNT + 27, slots.size(), false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, STORAGE_SLOT_COUNT, STORAGE_SLOT_COUNT + 27, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        broadcastChanges();
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return storage != null && storage.stillValid(player);
    }

    private static class AmmoSlot extends Slot {
        private final AmmoStorageBlockEntity storage;
        private final boolean clientSide;

        private AmmoSlot(Container container, AmmoStorageBlockEntity storage, boolean clientSide, int slot, int x, int y) {
            super(container, slot, x, y);
            this.storage = storage;
            this.clientSide = clientSide;
        }

        @Override
        public ItemStack getItem() {
            if (clientSide && storage != null) {
                ItemStack stack = storage.getSampleStack(getSlotIndex());
                if (!stack.isEmpty()) {
                    stack.setCount(1);
                }
                return stack;
            }
            return super.getItem();
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return AmmoStorageHelper.isValidAmmo(stack);
        }

        @Override
        public ItemStack safeInsert(ItemStack stack, int increment) {
            if (storage == null || stack.isEmpty() || !mayPlace(stack)) {
                return stack;
            }
            ItemStack insertedStack = stack.copy();
            insertedStack.setCount(Math.min(stack.getCount(), increment));
            int inserted = storage.insertAmmo(insertedStack, false);
            stack.shrink(inserted);
            return stack;
        }

        @Override
        public void set(ItemStack stack) {
            setByPlayer(stack);
        }

        @Override
        public void setByPlayer(ItemStack stack) {
            if (storage == null) {
                super.setByPlayer(stack);
                return;
            }
            int slot = getSlotIndex();
            ItemStack sample = storage.getSampleStack(slot);
            if (sample.isEmpty()) {
                storage.insertAmmo(stack, false);
                return;
            }
            if (stack.isEmpty()) {
                return;
            }
            if (!ItemStack.isSameItemSameTags(sample, stack)) {
                return;
            }
            int visibleCount = storage.getItem(slot).getCount();
            int added = stack.getCount() - visibleCount;
            if (added > 0) {
                ItemStack insertedStack = stack.copy();
                insertedStack.setCount(added);
                storage.insertAmmo(insertedStack, false);
            }
        }

        @Override
        public int getMaxStackSize() {
            return Integer.MAX_VALUE;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            int slotStacks = storage == null ? com.tacz.guns.block.AmmoStorageBlock.IRON_SLOT_STACKS : storage.getSlotStacks();
            return com.tacz.guns.util.AmmoStorageHelper.getStorageSlotLimit(stack, slotStacks);
        }
    }
}
