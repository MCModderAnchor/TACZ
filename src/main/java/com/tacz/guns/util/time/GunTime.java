package com.tacz.guns.util.time;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.time.AnimationClock;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.time.AnimationClocks;

/**
 * The time source for gun gameplay and animation. On a physical client this
 * follows the single-player pause; on a dedicated server it keeps running.
 */
public final class GunTime {
    // Keep the legacy -1 "never happened" timestamp well outside ordinary Lua time windows.
    private static final long EPOCH_OFFSET_NANOS = 100_000L * 1_000_000L;
    private static final AnimationClock CLOCK = AnimationClocks.client();
    private static volatile long originNanos = CLOCK.nowNanos();
    private static volatile long epoch;

    private GunTime() {
    }

    /** A nonnegative, monotonic timestamp within the current clock epoch. */
    public static long nowNanos() {
        long raw = CLOCK.nowNanos();
        long origin = originNanos;
        if (raw < origin) {
            synchronized (GunTime.class) {
                if (raw < originNanos) {
                    originNanos = raw;
                    epoch++;
                }
                origin = originNanos;
            }
        }
        return EPOCH_OFFSET_NANOS + Math.max(0L, raw - origin);
    }

    public static long nowMillis() {
        return nowNanos() / 1_000_000L;
    }

    public static boolean shouldTick() {
        return CLOCK.shouldTick();
    }

    /** Changes when the client leaves a world and its animation clock resets. */
    public static long epoch() {
        nowNanos();
        return epoch;
    }

    public static synchronized void resetEpoch() {
        originNanos = CLOCK.nowNanos();
        epoch++;
    }

    /** An unset or future timestamp is outside the window. */
    public static boolean isWithin(long timestamp, long durationMillis) {
        if (timestamp < 0 || durationMillis <= 0) {
            return false;
        }
        long elapsed = nowMillis() - timestamp;
        return elapsed >= 0 && elapsed < durationMillis;
    }

    public static boolean hasElapsed(long timestamp, long durationMillis) {
        if (timestamp < 0) {
            return false;
        }
        long elapsed = nowMillis() - timestamp;
        return elapsed >= durationMillis;
    }
}
