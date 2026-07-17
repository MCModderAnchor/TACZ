package com.tacz.guns.util;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

import java.util.function.Supplier;

/** Limits shooter masking to the synchronous dispatch of a bullet damage event. */
public final class BulletDamageContext {
    private static final ThreadLocal<Integer> HIDDEN_SHOOTER_DEPTH = ThreadLocal.withInitial(() -> 0);

    private BulletDamageContext() {
    }

    public static boolean isShooterHidden() {
        return HIDDEN_SHOOTER_DEPTH.get() > 0;
    }

    public static DamageSource withoutShooter(DamageSource source) {
        Entity directEntity = source.getDirectEntity();
        if (directEntity == source.getEntity()) {
            directEntity = null;
        }
        return new DamageSource(source.typeHolder(), directEntity, null);
    }

    public static <T> T runWithoutShooter(Supplier<T> action) {
        int previousDepth = HIDDEN_SHOOTER_DEPTH.get();
        HIDDEN_SHOOTER_DEPTH.set(previousDepth + 1);
        try {
            return action.get();
        } finally {
            if (previousDepth == 0) {
                HIDDEN_SHOOTER_DEPTH.remove();
            } else {
                HIDDEN_SHOOTER_DEPTH.set(previousDepth);
            }
        }
    }
}
