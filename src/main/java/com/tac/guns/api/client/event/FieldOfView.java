package com.tac.guns.api.client.event;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraftforge.client.event.EntityViewRenderEvent;

public class FieldOfView extends EntityViewRenderEvent {
    private final boolean isItemWithHand;
    private double fov;

    public FieldOfView(GameRenderer renderer, Camera camera, double partialTicks, double fov, boolean isItemWithHand) {
        super(renderer, camera, partialTicks);
        this.fov = fov;
        this.isItemWithHand = isItemWithHand;
    }

    public double getFOV() {
        return fov;
    }

    public void setFOV(double fov) {
        this.fov = fov;
    }

    public boolean isItemWithHand() {
        return isItemWithHand;
    }
}
