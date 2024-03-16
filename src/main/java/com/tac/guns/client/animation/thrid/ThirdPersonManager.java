package com.tac.guns.client.animation.thrid;

import com.google.common.collect.Maps;
import com.tac.guns.api.client.animation.IThirdPersonAnimation;

import java.util.Map;

public final class ThirdPersonManager {
    private static final Map<String, IThirdPersonAnimation> CACHE = Maps.newHashMap();
    private static final IThirdPersonAnimation EMPTY = new EmptyAnimation();

    public static void registerInner() {
        register("empty", EMPTY);
        register("rifle", new RifleAnimation());
        register("pistol", new PistolAnimation());
    }

    public static void register(String name, IThirdPersonAnimation animation) {
        CACHE.put(name, animation);
    }

    public static IThirdPersonAnimation getAnimation(String name) {
        return CACHE.getOrDefault(name, EMPTY);
    }
}
