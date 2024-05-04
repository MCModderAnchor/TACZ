package com.tacz.guns.client.resource.pojo.animation.bedrock;

import com.mojang.math.Vector3f;
import it.unimi.dsi.fastutil.doubles.Double2ObjectLinkedOpenHashMap;

import javax.annotation.Nullable;

@SuppressWarnings("MapOrSetKeyShouldOverrideHashCodeEquals")
public class AnimationKeyframes {
    private final Double2ObjectLinkedOpenHashMap<Keyframe> keyframes;

    public AnimationKeyframes(Double2ObjectLinkedOpenHashMap<Keyframe> keyframes) {
        this.keyframes = keyframes;
    }

    public Double2ObjectLinkedOpenHashMap<Keyframe> getKeyframes() {
        return keyframes;
    }

    public record Keyframe(@Nullable Vector3f pre, @Nullable Vector3f post, @Nullable Vector3f data,
                           @Nullable String lerpMode) {
    }
}
