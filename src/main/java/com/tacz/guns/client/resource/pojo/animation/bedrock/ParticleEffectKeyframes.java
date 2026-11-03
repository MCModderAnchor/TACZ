package com.tacz.guns.client.resource.pojo.animation.bedrock;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.ParticleEffectData;
import it.unimi.dsi.fastutil.doubles.Double2ObjectRBTreeMap;

/**
 * 基岩版动画 {@code particle_effects} 通道关键帧。
 * <p>
 * 键为时间（秒），值为 {@link ParticleEffectData}（效果 id + 定位骨骼名）。
 */
public class ParticleEffectKeyframes {
    private final Double2ObjectRBTreeMap<ParticleEffectData> keyframes;

    public ParticleEffectKeyframes(Double2ObjectRBTreeMap<ParticleEffectData> keyframes) {
        this.keyframes = keyframes;
    }

    public Double2ObjectRBTreeMap<ParticleEffectData> getKeyframes() {
        return keyframes;
    }
}
