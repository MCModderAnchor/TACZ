package com.tacz.guns.block;

import net.minecraft.util.StringRepresentable;
import net.minecraft.core.Direction;

public enum PegboardShape implements StringRepresentable {
    STRAIGHT("straight"),
    CORNER_LEFT("corner_left"),
    CORNER_RIGHT("corner_right");

    private final String name;

    PegboardShape(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public Direction getCornerFacing(Direction facing) {
        return switch (this) {
            case CORNER_LEFT -> facing.getCounterClockWise();
            case CORNER_RIGHT -> facing.getClockWise();
            case STRAIGHT -> facing;
        };
    }
}
