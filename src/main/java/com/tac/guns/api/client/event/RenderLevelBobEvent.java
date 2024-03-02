package com.tac.guns.api.client.event;

import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

public class RenderLevelBobEvent extends Event {
    @Cancelable
    public static class BobHurt extends RenderLevelBobEvent {
    }

    @Cancelable
    public static class BobView extends RenderLevelBobEvent {
    }

    /**
     * 使用注解也可以，但是热重载会导致游戏崩溃
     */
    @Override
    public boolean isCancelable() {
        return true;
    }
}
