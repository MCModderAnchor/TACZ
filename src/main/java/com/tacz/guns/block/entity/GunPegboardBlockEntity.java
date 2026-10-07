package com.tacz.guns.block.entity;

import com.tacz.guns.block.GunPegboardBlock;
import com.tacz.guns.block.GunPegboardSlotHelper;
import com.tacz.guns.block.PegboardShape;
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

import java.util.Map;

public class GunPegboardBlockEntity extends BlockEntity {
    public static final int GRID_SIZE = 4;
    public static final int SLOTS_PER_FACE = GRID_SIZE * GRID_SIZE;
    // 原有 0..15 槽位保持不变，16..31 属于折面；转角时每面仅启用三列。
    public static final int SLOT_COUNT = SLOTS_PER_FACE * 2;
    public static final BlockEntityType<GunPegboardBlockEntity> TYPE = BlockEntityType.Builder.of(GunPegboardBlockEntity::new, ModBlocks.GUN_PEGBOARD.get(), ModBlocks.GLOW_GUN_PEGBOARD.get()).build(null);
    private static final String ITEMS_TAG = "Items";
    private static final String SLOT_TAG = "Slot";
    private static final String ITEM_TAG = "Item";
    private static final String ROTATIONS_TAG = "Rotations";
    private static final String SLOT_SHAPE_TAG = "SlotShape";
    private static final String COLOR_TAG = "Color";
    private static final String INHERITED_COLOR_TAG = "InheritedColor";

    private final ItemStack[] items = new ItemStack[SLOT_COUNT];
    private final byte[] rotations = new byte[SLOT_COUNT];
    private int color = -1;
    private boolean inheritedColor;
    private final DisplayGunSlot[] displaySlots = new DisplayGunSlot[SLOT_COUNT];

    public GunPegboardBlockEntity(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
        for (int i = 0; i < items.length; i++) {
            items[i] = ItemStack.EMPTY;
        }
    }

    public ItemStack getItem(int slot) {
        return GunPegboardSlotHelper.isSlotActive(getBlockState(), slot) ? items[slot] : ItemStack.EMPTY;
    }

    public int getColor() {
        return color;
    }

    public boolean hasInheritedColor() {
        return inheritedColor;
    }

    public void setColor(int color, boolean inherited) {
        int normalized = color < 0 ? -1 : color & 0xFFFFFF;
        boolean inheritedColor = normalized >= 0 && inherited;
        if (this.color == normalized && this.inheritedColor == inheritedColor) return;
        this.color = normalized;
        this.inheritedColor = inheritedColor;
        if (level != null && !level.isClientSide()) {
            level.setBlock(worldPosition, getBlockState().setValue(GunPegboardBlock.DYED, this.color >= 0), Block.UPDATE_ALL);
        }
        update();
    }

    public int getRotation(int slot) {
        return isValidSlot(slot) ? rotations[slot] & 7 : 0;
    }

    public DisplayGunSlot getDisplaySlot(int slot) {
        if (!isValidSlot(slot)) throw new IndexOutOfBoundsException(slot);
        DisplayGunSlot handle = displaySlots[slot];
        if (handle == null) displaySlots[slot] = handle = new DisplayGunSlot(this, slot);
        return handle;
    }

    private void displaySlotChanged(int slot) {
        if (displaySlots[slot] != null) displaySlots[slot].changed();
    }

    public boolean setItem(int slot, ItemStack stack) {
        if (!GunPegboardSlotHelper.isSlotActive(getBlockState(), slot) || !items[slot].isEmpty()) {
            return false;
        }
        ItemStack stored = stack.copy();
        stored.setCount(1);
        items[slot] = stored;
        rotations[slot] = 0;
        displaySlotChanged(slot);
        update();
        return true;
    }

    /** Fill empty slots in one update when creating a display wall. */
    public boolean setItems(Map<Integer, ItemStack> stacks) {
        if (stacks.isEmpty() || stacks.size() > SLOT_COUNT) {
            return false;
        }
        for (Map.Entry<Integer, ItemStack> entry : stacks.entrySet()) {
            int slot = entry.getKey();
            if (!GunPegboardSlotHelper.isSlotActive(getBlockState(), slot) || entry.getValue().isEmpty() || !items[slot].isEmpty()) {
                return false;
            }
        }
        for (Map.Entry<Integer, ItemStack> entry : stacks.entrySet()) {
            int slot = entry.getKey();
            ItemStack stored = entry.getValue().copy();
            stored.setCount(1);
            items[slot] = stored;
            rotations[slot] = 0;
            displaySlotChanged(slot);
        }
        update();
        return true;
    }

    public boolean rotateItem(int slot) {
        if (!GunPegboardSlotHelper.isSlotActive(getBlockState(), slot) || items[slot].isEmpty()) {
            return false;
        }
        rotations[slot] = (byte) ((rotations[slot] + 1) & 7);
        if (displaySlots[slot] != null) displaySlots[slot].transformed();
        update();
        return true;
    }

    public boolean dropItem(int slot) {
        if (!isValidSlot(slot) || items[slot].isEmpty() || level == null) {
            return false;
        }
        Direction facing = GunPegboardSlotHelper.getSlotFacing(getBlockState(), slot);
        Block.popResource(level, worldPosition.relative(facing), items[slot]);
        items[slot] = ItemStack.EMPTY;
        rotations[slot] = 0;
        displaySlotChanged(slot);
        update();
        return true;
    }

    public boolean removeItem(int slot) {
        if (!isValidSlot(slot) || items[slot].isEmpty()) {
            return false;
        }
        items[slot] = ItemStack.EMPTY;
        rotations[slot] = 0;
        displaySlotChanged(slot);
        update();
        return true;
    }

    public void dropAllItems() {
        if (level == null) {
            return;
        }
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (!items[i].isEmpty()) {
                Block.popResource(level, worldPosition.relative(GunPegboardSlotHelper.getSlotFacing(getBlockState(), i)), items[i]);
                items[i] = ItemStack.EMPTY;
                rotations[i] = 0;
                displaySlotChanged(i);
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
    public void setBlockState(BlockState state) {
        BlockState previous = getBlockState();
        super.setBlockState(state);
        if (previous.getValue(GunPegboardBlock.FACING) != state.getValue(GunPegboardBlock.FACING)
                || previous.getValue(GunPegboardBlock.SHAPE) != state.getValue(GunPegboardBlock.SHAPE)) {
            boolean changed = false;
            for (int slot = 0; slot < SLOT_COUNT; slot++) {
                if (displaySlots[slot] != null) displaySlots[slot].transformed();
                boolean removedFace = slot >= SLOTS_PER_FACE
                        && previous.getValue(GunPegboardBlock.SHAPE) != state.getValue(GunPegboardBlock.SHAPE);
                if (level != null && !level.isClientSide() && !items[slot].isEmpty()
                        && (!GunPegboardSlotHelper.isSlotActive(state, slot) || removedFace)) {
                    Block.popResource(level, worldPosition.relative(GunPegboardSlotHelper.getSlotFacing(previous, slot)), items[slot]);
                    items[slot] = ItemStack.EMPTY;
                    rotations[slot] = 0;
                    displaySlotChanged(slot);
                    changed = true;
                }
            }
            if (changed) update();
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        color = tag.contains(COLOR_TAG, Tag.TAG_INT) ? tag.getInt(COLOR_TAG) & 0xFFFFFF : -1;
        inheritedColor = color >= 0 && tag.getBoolean(INHERITED_COLOR_TAG);
        ItemStack[] previousItems = items.clone();
        byte[] previousRotations = rotations.clone();
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
        PegboardShape shape = getBlockState().getValue(GunPegboardBlock.SHAPE);
        String savedShape = tag.getString(SLOT_SHAPE_TAG);
        // 结构镜像只转换方块状态；左右转角互换时，同步镜像每面的槽位列和旋转角度。
        boolean mirrored = shape == PegboardShape.CORNER_LEFT
                && savedShape.equals(PegboardShape.CORNER_RIGHT.getSerializedName())
                || shape == PegboardShape.CORNER_RIGHT
                && savedShape.equals(PegboardShape.CORNER_LEFT.getSerializedName());
        if (mirrored) {
            ItemStack[] savedItems = items.clone();
            byte[] savedRotations = rotations.clone();
            for (int slot = 0; slot < SLOT_COUNT; slot++) {
                int mirroredSlot = slot - slot % GRID_SIZE + GRID_SIZE - 1 - slot % GRID_SIZE;
                items[mirroredSlot] = savedItems[slot];
                rotations[mirroredSlot] = (byte) (-savedRotations[slot] & 7);
            }
        }
        for (int i = 0; i < displaySlots.length; i++) {
            DisplayGunSlot handle = displaySlots[i];
            if (handle == null) continue;
            if (!ItemStack.matches(previousItems[i], items[i])) handle.changed();
            else if (previousRotations[i] != rotations[i]) handle.transformed();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString(SLOT_SHAPE_TAG, getBlockState().getValue(GunPegboardBlock.SHAPE).getSerializedName());
        if (color >= 0) tag.putInt(COLOR_TAG, color);
        if (inheritedColor) tag.putBoolean(INHERITED_COLOR_TAG, true);
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
        // 网络更新不能按结构镜像解释，客户端可能尚未收到对应的方块状态。
        tag.remove(SLOT_SHAPE_TAG);
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
