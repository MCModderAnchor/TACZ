package com.tacz.guns.block;

import com.tacz.guns.block.entity.GunPegboardBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class GunPegboardSlotHelper {
    private static final double PLATE_THICKNESS = 3.0 / 16.0;
    private static final double LINE_OFFSET = 0.004;
    private static final double LINE_DEPTH = 0.0125;

    private GunPegboardSlotHelper() {
    }

    public static int getSlot(BlockState state, BlockPos pos, Vec3 hitLocation) {
        Direction facing = state.getValue(GunPegboardBlock.FACING);
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
        int col = Math.min((int) (u * GunPegboardBlockEntity.GRID_SIZE), GunPegboardBlockEntity.GRID_SIZE - 1);
        int row = Math.min((int) (y * GunPegboardBlockEntity.GRID_SIZE), GunPegboardBlockEntity.GRID_SIZE - 1);
        return row * GunPegboardBlockEntity.GRID_SIZE + col;
    }

    public static AABB getSlotAabb(BlockState state, BlockPos pos, int slot) {
        int row = slot / GunPegboardBlockEntity.GRID_SIZE;
        int col = slot % GunPegboardBlockEntity.GRID_SIZE;
        double minU = col / (double) GunPegboardBlockEntity.GRID_SIZE;
        double maxU = (col + 1) / (double) GunPegboardBlockEntity.GRID_SIZE;
        double minY = row / (double) GunPegboardBlockEntity.GRID_SIZE;
        double maxY = (row + 1) / (double) GunPegboardBlockEntity.GRID_SIZE;
        Direction facing = state.getValue(GunPegboardBlock.FACING);
        double x0;
        double x1;
        double z0;
        double z1;
        switch (facing) {
            case SOUTH -> {
                x0 = 1.0 - maxU;
                x1 = 1.0 - minU;
                z0 = PLATE_THICKNESS + LINE_OFFSET;
                z1 = PLATE_THICKNESS + LINE_OFFSET + LINE_DEPTH;
            }
            case EAST -> {
                x0 = PLATE_THICKNESS + LINE_OFFSET;
                x1 = PLATE_THICKNESS + LINE_OFFSET + LINE_DEPTH;
                z0 = minU;
                z1 = maxU;
            }
            case WEST -> {
                x0 = 1.0 - PLATE_THICKNESS - LINE_OFFSET - LINE_DEPTH;
                x1 = 1.0 - PLATE_THICKNESS - LINE_OFFSET;
                z0 = 1.0 - maxU;
                z1 = 1.0 - minU;
            }
            default -> {
                x0 = minU;
                x1 = maxU;
                z0 = 1.0 - PLATE_THICKNESS - LINE_OFFSET - LINE_DEPTH;
                z1 = 1.0 - PLATE_THICKNESS - LINE_OFFSET;
            }
        }
        return new AABB(pos.getX() + x0, pos.getY() + minY, pos.getZ() + z0, pos.getX() + x1, pos.getY() + maxY, pos.getZ() + z1).inflate(0.002);
    }
}
