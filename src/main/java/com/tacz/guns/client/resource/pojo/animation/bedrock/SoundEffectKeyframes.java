package com.tacz.guns.client.resource.pojo.animation.bedrock;

import it.unimi.dsi.fastutil.doubles.Double2ObjectRBTreeMap;
import net.minecraft.resources.ResourceLocation;

public class SoundEffectKeyframes {
    private final Double2ObjectRBTreeMap<ResourceLocation> keyframes;

    public SoundEffectKeyframes(Double2ObjectRBTreeMap<ResourceLocation> keyframes) {
        this.keyframes = keyframes;
    }

    public Double2ObjectRBTreeMap<ResourceLocation> getKeyframes() {
        return keyframes;
    }
}
