package com.tacz.guns.api.client.event;

import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

/**
 * 当第一人称视角触发摇晃时，玩家手部的摇晃
 */
public class RenderItemInHandBobEvent extends Event {
    /**
     * 使用注解也可以，但是热重载会导致游戏崩溃
     */
    @Override
    public boolean isCancelable() {
        return true;
    }

    @Cancelable
    public static class BobHurt extends RenderItemInHandBobEvent {
    }

    @Cancelable
    public static class BobView extends RenderItemInHandBobEvent {
    }
}
