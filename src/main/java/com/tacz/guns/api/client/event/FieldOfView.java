package com.tacz.guns.api.client.event;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraftforge.client.event.EntityViewRenderEvent;

/**
 * 在调用 GameRenderer#getFov 方法时触发该事件
 * 用于瞄准镜瞄准时 FOV 相关变化时调用
 */
public class FieldOfView extends EntityViewRenderEvent.FieldOfView {
    private final boolean isItemWithHand;

    public FieldOfView(GameRenderer renderer, Camera camera, double partialTicks, double fov, boolean isItemWithHand) {
        super(renderer, camera, partialTicks, fov);
        this.isItemWithHand = isItemWithHand;
    }

    public boolean isItemWithHand() {
        return isItemWithHand;
    }
}
