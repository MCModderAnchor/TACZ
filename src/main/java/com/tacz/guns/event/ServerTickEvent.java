package com.tacz.guns.event;

import com.tacz.guns.util.CycleTaskHelper;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class ServerTickEvent {
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        // 更新 CycleTaskHelper 中的任务
        CycleTaskHelper.tick();
    }
}
