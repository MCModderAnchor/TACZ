package com.tacz.guns.block.entity;

import com.tacz.guns.block.GunPegboardBlock;
import com.tacz.guns.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class GunPegboardBlockEntity extends BlockEntity {
    public static final int GRID_SIZE = 4;
    public static final int SLOT_COUNT = GRID_SIZE * GRID_SIZE;
    public static final BlockEntityType<GunPegboardBlockEntity> TYPE = BlockEntityType.Builder.of(GunPegboardBlockEntity::new, ModBlocks.GUN_PEGBOARD.get(), ModBlocks.GLOW_GUN_PEGBOARD.get()).build(null);
    private static final String ITEMS_TAG = "Items";
    private static final String SLOT_TAG = "Slot";
    private static final String ITEM_TAG = "Item";
    private static final String ROTATIONS_TAG = "Rotations";

    private final ItemStack[] items = new ItemStack[SLOT_COUNT];
    private final byte[] rotations = new byte[SLOT_COUNT];

    public GunPegboardBlockEntity(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
        for (int i = 0; i < items.length; i++) {
            items[i] = ItemStack.EMPTY;
        }
    }

    public ItemStack getItem(int slot) {
        return isValidSlot(slot) ? items[slot] : ItemStack.EMPTY;
    }

    public int getRotation(int slot) {
        return isValidSlot(slot) ? rotations[slot] & 7 : 0;
    }

    public boolean setItem(int slot, ItemStack stack) {
        if (!isValidSlot(slot) || !items[slot].isEmpty()) {
            return false;
        }
        ItemStack stored = stack.copy();
        stored.setCount(1);
        items[slot] = stored;
        rotations[slot] = 0;
        update();
        return true;
    }

    public boolean rotateItem(int slot) {
        if (!isValidSlot(slot) || items[slot].isEmpty()) {
            return false;
        }
        rotations[slot] = (byte) ((rotations[slot] + 1) & 7);
        update();
        return true;
    }

    public boolean dropItem(int slot) {
        if (!isValidSlot(slot) || items[slot].isEmpty() || level == null) {
            return false;
        }
        Direction facing = getBlockState().getValue(GunPegboardBlock.FACING);
        Block.popResource(level, worldPosition.relative(facing), items[slot]);
        items[slot] = ItemStack.EMPTY;
        rotations[slot] = 0;
        update();
        return true;
    }

    public boolean removeItem(int slot) {
        if (!isValidSlot(slot) || items[slot].isEmpty()) {
            return false;
        }
        items[slot] = ItemStack.EMPTY;
        rotations[slot] = 0;
        update();
        return true;
    }

    public void dropAllItems() {
        if (level == null) {
            return;
        }
        Direction facing = getBlockState().getValue(GunPegboardBlock.FACING);
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (!items[i].isEmpty()) {
                Block.popResource(level, worldPosition.relative(facing), items[i]);
                items[i] = ItemStack.EMPTY;
                rotations[i] = 0;
            }
        }
        update();
    }

    private void update() {
        setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
        }
    }

    private static boolean isValidSlot(int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        for (int i = 0; i < items.length; i++) {
            items[i] = ItemStack.EMPTY;
            rotations[i] = 0;
        }
        if (tag.contains(ITEMS_TAG, Tag.TAG_LIST)) {
            ListTag list = tag.getList(ITEMS_TAG, Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                int slot = entry.getByte(SLOT_TAG) & 255;
                if (isValidSlot(slot) && entry.contains(ITEM_TAG, Tag.TAG_COMPOUND)) {
                    items[slot] = ItemStack.of(entry.getCompound(ITEM_TAG));
                }
            }
        }
        if (tag.contains(ROTATIONS_TAG, Tag.TAG_BYTE_ARRAY)) {
            byte[] saved = tag.getByteArray(ROTATIONS_TAG);
            System.arraycopy(saved, 0, rotations, 0, Math.min(saved.length, rotations.length));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ListTag list = new ListTag();
        for (int i = 0; i < items.length; i++) {
            if (!items[i].isEmpty()) {
                CompoundTag entry = new CompoundTag();
                entry.putByte(SLOT_TAG, (byte) i);
                entry.put(ITEM_TAG, items[i].save(new CompoundTag()));
                list.add(entry);
            }
        }
        tag.put(ITEMS_TAG, list);
        tag.putByteArray(ROTATIONS_TAG, rotations);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.offset(-2, -2, -2), worldPosition.offset(3, 3, 3));
    }
}
