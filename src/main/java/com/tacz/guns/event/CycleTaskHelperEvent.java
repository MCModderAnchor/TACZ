package com.tacz.guns.event;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.LinkedList;
import java.util.List;
import java.util.function.BooleanSupplier;

@Mod.EventBusSubscriber
public class CycleTaskHelperEvent {
    private static final List<CycleTaskTicker> CYCLE_TASKS = new LinkedList<>();

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        // 迭代、调用并删除返回 false 的任务
        CYCLE_TASKS.removeIf(ticker -> !ticker.tick());
    }

    /**
     * 根据提供的时间间隔循环执行任务。会立刻调用一次。
     *
     * @param task     循环执行的任务，会根据返回的 boolean 值决定是否继续下一次循环。如果返回 false ，则将不再循环。
     * @param periodMs 循环调用的时间间隔，单位为毫秒。
     * @param cycles   最大循环次数。
     */
    public static void addCycleTask(BooleanSupplier task, long periodMs, int cycles) {
        CycleTaskTicker ticker = new CycleTaskTicker(task, periodMs, cycles);
        if (ticker.tick()) {
            CYCLE_TASKS.add(ticker);
        }
    }

    private static class CycleTaskTicker {
        private final BooleanSupplier task;
        private final float periodS;
        private final int cycles;
        private long timestamp = -1;
        private float compensation = 0;
        private int count = 0;

        private CycleTaskTicker(BooleanSupplier task, long periodMs, int cycles) {
            this.task = task;
            this.periodS = periodMs / 1000f;
            this.cycles = cycles;
        }

        private boolean tick() {
            if (timestamp == -1) {
                timestamp = System.currentTimeMillis();
                if (++count > cycles) {
                    return false;
                }
                return task.getAsBoolean();
            }
            float duration = (System.currentTimeMillis() - timestamp) / 1000f + compensation;
            if (duration > periodS) {
                compensation = duration;
                timestamp = System.currentTimeMillis();
                while (compensation > periodS) {
                    if (++count > cycles) {
                        return false;
                    }
                    if (!task.getAsBoolean()) {
                        return false;
                    }
                    compensation -= periodS;
                }
            }
            return true;
        }
    }
}
