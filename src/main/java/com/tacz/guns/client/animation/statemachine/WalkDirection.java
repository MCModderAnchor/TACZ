package com.tacz.guns.client.animation.statemachine;

import net.minecraft.client.player.Input;

public enum WalkDirection {
    FORWARD,
    SIDE_WAY,
    BACKWARD,
    NONE;

    public static WalkDirection fromInput(Input input) {
        if (input.up) {
            return FORWARD;
        }
        if (input.down) {
            return BACKWARD;
        }
        if (input.left || input.right) {
            return SIDE_WAY;
        }
        return NONE;
    }
}
