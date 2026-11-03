package com.tacz.guns.block;

import com.tacz.guns.block.entity.GunDisplayCaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class GunDisplayCaseSlotHelper {
    private static final double BACK_PANEL_THICKNESS = 3.0 / 16.0;
    private static final double LINE_OFFSET = 0.004;
    private static final double LINE_DEPTH = 0.0125;

    private GunDisplayCaseSlotHelper() {
    }

    public static int getSlot(BlockState state, BlockPos pos, Vec3 hitLocation) {
        Direction facing = state.getValue(GunDisplayCaseBlock.FACING);
        double x = hitLocation.x - pos.getX();
        double y = hitLocation.y - pos.getY();
        double z = hitLocation.z - pos.getZ();
        double u = switch (facing) {
            case SOUTH -> 1.0 - x;
            case EAST -> z;
            case WEST -> 1.0 - z;
            default -> x;
        };
        if (u < 0 || u >= 1 || y < 0 || y >= 1) {
            return -1;
        }
        int col = Math.min((int) (u * GunDisplayCaseBlockEntity.GRID_SIZE), GunDisplayCaseBlockEntity.GRID_SIZE - 1);
        int row = Math.min((int) (y * GunDisplayCaseBlockEntity.GRID_SIZE), GunDisplayCaseBlockEntity.GRID_SIZE - 1);
        return row * GunDisplayCaseBlockEntity.GRID_SIZE + col;
    }

    public static AABB getSlotAabb(BlockState state, BlockPos pos, int slot) {
        int row = slot / GunDisplayCaseBlockEntity.GRID_SIZE;
        int col = slot % GunDisplayCaseBlockEntity.GRID_SIZE;
        double minU = col / (double) GunDisplayCaseBlockEntity.GRID_SIZE;
        double maxU = (col + 1) / (double) GunDisplayCaseBlockEntity.GRID_SIZE;
        double minY = row / (double) GunDisplayCaseBlockEntity.GRID_SIZE;
        double maxY = (row + 1) / (double) GunDisplayCaseBlockEntity.GRID_SIZE;
        Direction facing = state.getValue(GunDisplayCaseBlock.FACING);
        double x0;
        double x1;
        double z0;
        double z1;
        switch (facing) {
            case SOUTH -> {
                x0 = 1.0 - maxU;
                x1 = 1.0 - minU;
                z0 = BACK_PANEL_THICKNESS + LINE_OFFSET;
                z1 = BACK_PANEL_THICKNESS + LINE_OFFSET + LINE_DEPTH;
            }
            case EAST -> {
                x0 = BACK_PANEL_THICKNESS + LINE_OFFSET;
                x1 = BACK_PANEL_THICKNESS + LINE_OFFSET + LINE_DEPTH;
                z0 = minU;
                z1 = maxU;
            }
            case WEST -> {
                x0 = 1.0 - BACK_PANEL_THICKNESS - LINE_OFFSET - LINE_DEPTH;
                x1 = 1.0 - BACK_PANEL_THICKNESS - LINE_OFFSET;
                z0 = 1.0 - maxU;
                z1 = 1.0 - minU;
            }
            default -> {
                x0 = minU;
                x1 = maxU;
                z0 = 1.0 - BACK_PANEL_THICKNESS - LINE_OFFSET - LINE_DEPTH;
                z1 = 1.0 - BACK_PANEL_THICKNESS - LINE_OFFSET;
            }
        }
        return new AABB(pos.getX() + x0, pos.getY() + minY, pos.getZ() + z0, pos.getX() + x1, pos.getY() + maxY, pos.getZ() + z1).inflate(0.002);
    }
}
