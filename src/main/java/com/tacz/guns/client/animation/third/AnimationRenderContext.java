package com.tacz.guns.client.animation.third;

/** A drawing scope; it never changes an entity's animation state. */
public final class AnimationRenderContext implements AutoCloseable {
    private static final ThreadLocal<AnimationRenderContext> CURRENT = new ThreadLocal<>();

    private final AnimationRenderContext previous;
    private final Object entity;

    private AnimationRenderContext(Object entity) {
        this.previous = CURRENT.get();
        this.entity = entity;
        CURRENT.set(this);
    }

    public static AnimationRenderContext hand(Object entity) {
        return new AnimationRenderContext(entity);
    }

    public static boolean isFirstPersonHand(Object entity) {
        for (AnimationRenderContext context = CURRENT.get(); context != null; context = context.previous) {
            if (context.entity == entity) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void close() {
        if (previous == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(previous);
        }
    }
}
