package com.tacz.guns.client.resource.pojo.animation.bedrock;


import it.unimi.dsi.fastutil.doubles.Double2ObjectRBTreeMap;
import org.joml.Vector3f;

import javax.annotation.Nullable;

@SuppressWarnings("MapOrSetKeyShouldOverrideHashCodeEquals")
public class AnimationKeyframes {
    private final Double2ObjectRBTreeMap<Keyframe> keyframes;

    public AnimationKeyframes(Double2ObjectRBTreeMap<Keyframe> keyframes) {
        this.keyframes = keyframes;
    }

    public Double2ObjectRBTreeMap<Keyframe> getKeyframes() {
        return keyframes;
    }

    public record Keyframe(@Nullable Vector3f pre, @Nullable Vector3f post, @Nullable Vector3f data,
                           @Nullable String lerpMode) {
    }
}
