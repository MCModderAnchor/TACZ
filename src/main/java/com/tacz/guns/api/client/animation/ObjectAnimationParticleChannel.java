package com.tacz.guns.api.client.animation;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.ParticleEffectData;

import java.util.Arrays;
import java.util.List;

/**
 * 动画 {@code particle_effects} 通道。在动画播放区间内收集跨过关键帧的粒子请求。
 * <p>
 * 与 {@link ObjectAnimationSoundChannel} 同模式：时间区间左开右闭，支持循环回绕。
 */
public class ObjectAnimationParticleChannel {
    public double[] keyframeTimeS;
    public ParticleEffectData[] keyframeData;

    public ObjectAnimationParticleChannel() {
    }

    public ObjectAnimationParticleChannel(ObjectAnimationParticleChannel source) {
        this.keyframeTimeS = source.keyframeTimeS;
        this.keyframeData = source.keyframeData;
    }

    /**
     * 收集 (fromTimeS, toTimeS] 区间内跨过的关键帧粒子请求。
     */
    public void collectParticles(double fromTimeS, double toTimeS, List<ParticleEffectData> sink) {
        if (keyframeTimeS == null || keyframeTimeS.length == 0 || fromTimeS == toTimeS) {
            return;
        }
        if (fromTimeS > toTimeS && fromTimeS <= getEndTimeS()) {
            // 循环回绕：先播放 [0, to]，再播放 [from, end]
            collect(0, toTimeS, sink);
            toTimeS = getEndTimeS();
        }
        int from = computeIndex(fromTimeS, true);
        int to = computeIndex(toTimeS, false);
        for (int i = from + 1; i <= to; i++) {
            sink.add(keyframeData[i]);
        }
    }

    private void collect(double fromTimeS, double toTimeS, List<ParticleEffectData> sink) {
        int from = computeIndex(fromTimeS, true);
        int to = computeIndex(toTimeS, false);
        for (int i = from + 1; i <= to; i++) {
            sink.add(keyframeData[i]);
        }
    }

    public double getEndTimeS() {
        return keyframeTimeS[keyframeTimeS.length - 1];
    }

    private int computeIndex(double timeS, boolean open) {
        int index = Arrays.binarySearch(keyframeTimeS, timeS);
        if (index >= 0) {
            return open ? index - 1 : index;
        }
        return -index - 2;
    }
}
