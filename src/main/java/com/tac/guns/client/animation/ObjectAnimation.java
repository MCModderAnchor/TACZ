package com.tac.guns.client.animation;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * Create a {@link ObjectAnimationRunner} instance to run a {@link ObjectAnimation}
 */
public class ObjectAnimation {
    public final String name;
    /**
     * key of this map is node name.
     */
    private final Map<String, List<ObjectAnimationChannel>> channels = new HashMap<>();
    public @Nonnull PlayType playType = PlayType.PLAY_ONCE_HOLD;
    /**
     * The current time, in nanoseconds
     */
    public long timeNs = 0;
    /**
     * The maximum {@link ObjectAnimationChannel#getEndTimeS()} of all channels
     */
    private float maxEndTimeS = 0f;

    public ObjectAnimation(@Nonnull String name) {
        this.name = Objects.requireNonNull(name);
    }

    /**
     * Create a copy of source object animation,
     * The values of the new object animation is the same as the source,
     * but the new one won't hold any Animation Listener.
     */
    public ObjectAnimation(ObjectAnimation source) {
        this.name = source.name;
        this.playType = source.playType;
        this.maxEndTimeS = source.maxEndTimeS;
        this.timeNs = source.timeNs;
        for (Map.Entry<String, List<ObjectAnimationChannel>> entry : source.channels.entrySet()) {
            List<ObjectAnimationChannel> newList = new ArrayList<>();
            for (ObjectAnimationChannel channel : entry.getValue()) {
                ObjectAnimationChannel newChannel = new ObjectAnimationChannel(channel.type, channel.content);
                newChannel.node = channel.node;
                newList.add(newChannel);
            }
            this.channels.put(entry.getKey(), newList);
        }
    }

    public void addChannel(ObjectAnimationChannel channel) {
        channels.compute(channel.node, (node, list) -> {
            if (list == null) list = new ArrayList<>();
            list.add(channel);
            return list;
        });

        if (channel.getEndTimeS() > maxEndTimeS)
            maxEndTimeS = channel.getEndTimeS();
    }

    public void removeChannel(ObjectAnimationChannel channel) {
        channels.compute(channel.node, (node, list) -> {
            if (list == null) return null;
            list.remove(channel);
            return list;
        });

        maxEndTimeS = 0.0f;
        for (List<ObjectAnimationChannel> channels : channels.values()) {
            for (ObjectAnimationChannel c : channels) {
                maxEndTimeS = Math.max(maxEndTimeS, c.getEndTimeS());
            }
        }
    }

    public Map<String, List<ObjectAnimationChannel>> getChannels() {
        return channels;
    }

    public void applyAnimationListeners(AnimationListenerSupplier supplier) {
        for (List<ObjectAnimationChannel> channelList : channels.values()) {
            for (ObjectAnimationChannel channel : channelList) {
                AnimationListener listener = supplier.supplyListeners(channel.node, channel.type);
                if (listener != null)
                    channel.addListener(listener);
            }
        }
    }

    /**
     * Trigger all listeners to notify them of the updated value.
     */
    public void update() {
        for (List<ObjectAnimationChannel> channels : channels.values()) {
            for (ObjectAnimationChannel channel : channels) {
                channel.update(timeNs / 1e9f);
            }
        }
    }

    public float getMaxEndTimeS() {
        return maxEndTimeS;
    }

    public enum PlayType {
        PLAY_ONCE_HOLD,
        LOOP
    }
}
