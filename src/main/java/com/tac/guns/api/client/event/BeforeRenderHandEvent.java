package com.tac.guns.api.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraftforge.eventbus.api.Event;

public class BeforeRenderHandEvent extends Event {
    private final PoseStack poseStack;

    public BeforeRenderHandEvent(PoseStack poseStack) {
        this.poseStack = poseStack;
    }

    public PoseStack getPoseStack() {
        return poseStack;
    }
}
