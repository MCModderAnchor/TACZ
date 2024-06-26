package com.tacz.guns.compat.playeranimator.animation;

import com.google.common.collect.Maps;
import com.tacz.guns.GunMod;
import com.tacz.guns.compat.playeranimator.Condition;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.data.gson.AnimationSerializing;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public enum PlayerAnimatorAssetManager {
    INSTANCE;

    private final HashMap<ResourceLocation, EnumMap<Condition, KeyframeAnimation>> animations = new HashMap<>();

    void putAnimation(ResourceLocation id, InputStream stream) throws IOException {
        List<KeyframeAnimation> keyframeAnimations = AnimationSerializing.deserializeAnimation(stream);
        for (var animation : keyframeAnimations) {
            String name = PlayerAnimationRegistry.serializeTextToString((String) animation.extraData.get("name")).toUpperCase(Locale.ENGLISH);
            try {
                Condition condition = Condition.valueOf(name);
                animations.computeIfAbsent(id, k -> Maps.newEnumMap(Condition.class)).put(condition, animation);
            } catch (IllegalArgumentException e) {
                GunMod.LOGGER.error(e.getMessage());
            }
        }
    }

    Optional<KeyframeAnimation> getAnimations(ResourceLocation id, Condition condition) {
        var animationEnumMap = this.animations.get(id);
        if (animationEnumMap == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(animationEnumMap.get(condition));
    }

    public boolean containsKey(ResourceLocation id) {
        return animations.containsKey(id);
    }

    public void clearAll() {
        animations.clear();
    }
}