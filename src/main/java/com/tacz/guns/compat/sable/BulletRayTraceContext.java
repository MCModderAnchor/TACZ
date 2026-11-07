package com.tacz.guns.compat.sable;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;

import java.util.function.Predicate;
import java.util.function.Supplier;

/** Keeps the bullet's block filter when Sable creates a new ClipContext for a sub-level. */
public final class BulletRayTraceContext {
    private static final ThreadLocal<Predicate<BlockState>> BLOCK_FILTER = new ThreadLocal<>();

    private BulletRayTraceContext() {
    }

    public static boolean ignores(BlockState state) {
        Predicate<BlockState> filter = BLOCK_FILTER.get();
        return filter != null && filter.test(state);
    }

    public static boolean ignores(BlockGetter level, BlockPos pos) {
        Predicate<BlockState> filter = BLOCK_FILTER.get();
        return filter != null && filter.test(level.getBlockState(pos));
    }

    public static <T> T trace(Predicate<BlockState> filter, Supplier<T> operation) {
        Predicate<BlockState> previous = BLOCK_FILTER.get();
        BLOCK_FILTER.set(filter);
        try {
            return operation.get();
        } finally {
            if (previous == null) {
                BLOCK_FILTER.remove();
            } else {
                BLOCK_FILTER.set(previous);
            }
        }
    }
}
