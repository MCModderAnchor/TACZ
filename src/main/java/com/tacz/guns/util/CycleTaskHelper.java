package com.tacz.guns.util;

import java.util.LinkedList;
import java.util.List;
import java.util.function.BooleanSupplier;

public final class CycleTaskHelper {
    private static final List<CycleTaskHelper.CycleTaskTicker> CYCLE_TASKS = new LinkedList<>();
    private static final List<CycleTaskHelper.CycleTaskTicker> TEMP_CYCLE_TASKS = new LinkedList<>();

    /**
     * 根据提供的时间间隔循环执行任务。会立刻调用一次。
     *
     * @param task     循环执行的任务，会根据返回的 boolean 值决定是否继续下一次循环。如果返回 false ，则将不再循环。
     * @param periodMs 循环调用的时间间隔，单位为毫秒。
     * @param cycles   最大循环次数。-1 代表无限次。
     */
    public static void addCycleTask(BooleanSupplier task, long periodMs, int cycles) {
        CycleTaskHelper.CycleTaskTicker ticker = new CycleTaskHelper.CycleTaskTicker(task, periodMs, cycles);
        if (ticker.tick()) {
            CYCLE_TASKS.add(ticker);
        }
    }

    public static void addCycleTask(BooleanSupplier task, long delayMs, long periodMs, int cycles) {
        if (delayMs <= 0) {
            addCycleTask(task, periodMs, cycles);
            return;
        }
        CycleTaskHelper.CycleTaskTicker ticker = new CycleTaskHelper.CycleTaskTicker(task, delayMs, periodMs, cycles);
        CYCLE_TASKS.add(ticker);
    }

    public static void tick() {
        TEMP_CYCLE_TASKS.addAll(CYCLE_TASKS);
        CYCLE_TASKS.clear();
        TEMP_CYCLE_TASKS.removeIf(ticker -> !ticker.tick());
        CYCLE_TASKS.addAll(TEMP_CYCLE_TASKS);
        TEMP_CYCLE_TASKS.clear();
    }

    private static class CycleTaskTicker {
        private final BooleanSupplier task;
        private final float periodS;
        private final int cycles;
        private float delayS = 0;
        private long timestamp = -1;
        private float compensation = 0;
        private int count = 0;

        private CycleTaskTicker(BooleanSupplier task, long periodMs, int cycles) {
            this.task = task;
            this.periodS = periodMs / 1000f;
            this.cycles = cycles;
        }

        private CycleTaskTicker(BooleanSupplier task, long delayMs, long periodMs, int cycles) {
            this.delayS = delayMs / 1000f;
            this.timestamp = System.currentTimeMillis();
            this.task = task;
            this.periodS = periodMs / 1000f;
            this.cycles = cycles;
        }

        private boolean tick() {
            if (timestamp == -1) {
                timestamp = System.currentTimeMillis();
                if (cycles > 0 && ++count > cycles) {
                    return false;
                }
                return task.getAsBoolean();
            }
            float duration = (System.currentTimeMillis() - timestamp) / 1000f + compensation;
            if (delayS > 0) {
                if (delayS > duration) {
                    // 延迟还没结束，减少延迟，继续tick
                    delayS = delayS - duration;
                    return true;
                } else {
                    // 延迟执行结束，将延迟设为 0
                    // 减少 duration 再加上一个 period，使得后续循环中 task 至少被执行一次。
                    delayS = 0;
                    duration = duration - delayS + periodS;
                }
            }
            if (duration > periodS) {
                compensation = duration;
                timestamp = System.currentTimeMillis();
                while (compensation > periodS) {
                    if (cycles > 0 && ++count > cycles) {
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
