package com.tacz.guns.api.client.event;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraftforge.client.event.ViewportEvent;

/**
 * 在调用 GameRenderer#getFov 方法时触发该事件
 * 用于瞄准镜瞄准时 FOV 相关变化时调用
 */
public class FieldOfView extends ViewportEvent {
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
