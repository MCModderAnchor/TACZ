package com.tacz.guns.api.client.animation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * 创建一个 {@link ObjectAnimationRunner} 实例以运行 {@link ObjectAnimation}
 */
public class ObjectAnimation {
    /**
     * 动画名称
     */
    public final String name;
    /**
     * 此 map 的 key 是节点名称
     */
    private final Map<String, List<ObjectAnimationChannel>> channels = new HashMap<>();
    private @Nullable ObjectAnimationSoundChannel soundChannel;
    /**
     * 播放类型
     */
    public @Nonnull PlayType playType = PlayType.PLAY_ONCE_HOLD;
    /**
     * 所有轨道的最大结束时间 {@link ObjectAnimationChannel#getEndTimeS()}
     */
    private float maxEndTimeS = 0f;

    protected ObjectAnimation(@Nonnull String name) {
        this.name = Objects.requireNonNull(name);
    }

    /**
     * 创建源对象动画的拷贝，
     * 新对象动画的值与源动画的值相同，
     * 但新对象动画不会包含任何动画监听器。
     */
    public ObjectAnimation(ObjectAnimation source) {
        this.name = source.name;
        this.playType = source.playType;
        this.maxEndTimeS = source.maxEndTimeS;
        for (Map.Entry<String, List<ObjectAnimationChannel>> entry : source.channels.entrySet()) {
            List<ObjectAnimationChannel> newList = new ArrayList<>();
            for (ObjectAnimationChannel channel : entry.getValue()) {
                ObjectAnimationChannel newChannel = new ObjectAnimationChannel(channel.type, channel.content);
                newChannel.node = channel.node;
                newChannel.interpolator = channel.interpolator;
                newList.add(newChannel);
            }
            this.channels.put(entry.getKey(), newList);
        }
        if (source.soundChannel != null) {
            this.soundChannel = new ObjectAnimationSoundChannel(source.soundChannel.content);
        }
    }

    protected void addChannel(ObjectAnimationChannel channel) {
        channels.compute(channel.node, (node, list) -> {
            if (list == null) {
                list = new ArrayList<>();
            }
            list.add(channel);
            return list;
        });
        if (channel.getEndTimeS() > maxEndTimeS) {
            maxEndTimeS = channel.getEndTimeS();
        }
    }

    protected void setSoundChannel(@Nonnull ObjectAnimationSoundChannel soundChannel) {
        if (soundChannel.getEndTimeS() > maxEndTimeS) {
            maxEndTimeS = (float) soundChannel.getEndTimeS();
        }
        this.soundChannel = soundChannel;
    }

    public Map<String, List<ObjectAnimationChannel>> getChannels() {
        return channels;
    }

    @Nullable
    public ObjectAnimationSoundChannel getSoundChannel() {
        return this.soundChannel;
    }

    public void applyAnimationListeners(AnimationListenerSupplier supplier) {
        for (List<ObjectAnimationChannel> channelList : channels.values()) {
            for (ObjectAnimationChannel channel : channelList) {
                AnimationListener listener = supplier.supplyListeners(channel.node, channel.type);
                if (listener != null) {
                    channel.addListener(listener);
                }
            }
        }
    }

    /**
     * 触发所有监听器，通知它们更新相关数值
     */
    public void update(boolean blend, float timeNs) {
        for (List<ObjectAnimationChannel> channels : channels.values()) {
            for (ObjectAnimationChannel channel : channels) {
                channel.update(timeNs / 1e9f, blend);
            }
        }
    }

    public float getMaxEndTimeS() {
        return maxEndTimeS;
    }

    public enum PlayType {
        /**
         * 播放一次，停留在最后一帧
         */
        PLAY_ONCE_HOLD,
        /**
         * 播放一次后停止
         */
        PLAY_ONCE_STOP,
        /**
         * 循环播放
         */
        LOOP
    }
}
