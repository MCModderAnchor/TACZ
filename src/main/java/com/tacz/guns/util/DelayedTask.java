package com.tacz.guns.util;

import com.google.common.collect.Lists;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.LinkedList;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * 延迟执行的工具类
 */
@OnlyIn(Dist.CLIENT)
public final class DelayedTask {
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    public static LinkedList<BooleanSupplier> SUPPLIERS = Lists.newLinkedList();

    private DelayedTask() {
    }

    public static void add(final Runnable runnable, final int delayedTick) {
        final int[] tickArray = {delayedTick};
        SUPPLIERS.add(() -> {
            if (--tickArray[0] < 0) {
                runnable.run();
                return true;
            }
            return false;
        });
    }

    public static void add(final Consumer<Integer> consumer, final int delayedTick, final int times) {
        final int[] tickArray = {delayedTick};
        SUPPLIERS.add(() -> {
            if (--tickArray[0] < 0) {
                consumer.accept(times);
                return true;
            }
            return false;
        });
    }
}
