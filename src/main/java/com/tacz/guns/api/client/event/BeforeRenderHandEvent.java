package com.tacz.guns.api.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraftforge.eventbus.api.Event;

/**
 * 在调用 ItemInHandRenderer#renderHandsWithItems 方法时触发该事件
 * 用于相机动画相关调用
 */
public class BeforeRenderHandEvent extends Event {
    private final PoseStack poseStack;

    public BeforeRenderHandEvent(PoseStack poseStack) {
        this.poseStack = poseStack;
    }

    public PoseStack getPoseStack() {
        return poseStack;
    }
}
