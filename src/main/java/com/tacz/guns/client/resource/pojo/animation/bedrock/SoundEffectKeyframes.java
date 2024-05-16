package com.tacz.guns.client.resource.pojo.animation.bedrock;

import it.unimi.dsi.fastutil.doubles.Double2ObjectLinkedOpenHashMap;
import net.minecraft.resources.ResourceLocation;

public class SoundEffectKeyframes {
    private final Double2ObjectLinkedOpenHashMap<ResourceLocation> keyframes;

    public SoundEffectKeyframes(Double2ObjectLinkedOpenHashMap<ResourceLocation> keyframes) {
        this.keyframes = keyframes;
    }

    public Double2ObjectLinkedOpenHashMap<ResourceLocation> getKeyframes() {
        return keyframes;
    }
}
