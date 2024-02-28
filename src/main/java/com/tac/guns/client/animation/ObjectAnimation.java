package com.tac.guns.client.animation;

import com.mojang.logging.LogUtils;
import com.tac.guns.client.animation.gltf.AccessorModel;
import com.tac.guns.client.animation.gltf.AnimationModel;
import com.tac.guns.client.animation.gltf.AnimationStructure;
import com.tac.guns.client.animation.gltf.NodeModel;
import com.tac.guns.client.animation.gltf.accessor.AccessorData;
import com.tac.guns.client.animation.gltf.accessor.AccessorFloatData;
import com.tac.guns.client.animation.interpolator.InterpolatorUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
     * The current playing progress time, in nanoseconds
     */
    public long timeNs = 0;
    /**
     * The maximum {@link ObjectAnimationChannel#getEndTimeS()} of all channels
     */
    private float maxEndTimeS = 0f;

    private ObjectAnimation(@Nonnull String name) {
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

    protected static @Nonnull List<ObjectAnimation> createAnimations(@Nonnull AnimationStructure structure, @Nullable AnimationListenerSupplier... suppliers) {
        List<ObjectAnimation> result = new ArrayList<>();

        List<AnimationModel> animationModels = structure.getAnimationModels();
        for (AnimationModel animationModel : animationModels) {
            ObjectAnimation animation = new ObjectAnimation(animationModel.getName());

            //init animation channels
            List<AnimationModel.Channel> channelModels = animationModel.getChannels();
            for (AnimationModel.Channel channelModel : channelModels) {
                ObjectAnimationChannel channel =
                        new ObjectAnimationChannel(
                                ObjectAnimationChannel.ChannelType.valueOf(channelModel.path().toUpperCase())
                        );
                AnimationModel.Sampler sampler = channelModel.sampler();

                //init channel's node name and interpolator
                AnimationModel.Interpolation interpolation = sampler.interpolation();
                NodeModel nodeModel = channelModel.nodeModel();
                //Quaternions require special interpolation
                if (channel.type.equals(ObjectAnimationChannel.ChannelType.ROTATION)
                        && interpolation.equals(AnimationModel.Interpolation.LINEAR)) {
                    channel.content.interpolator = InterpolatorUtil.fromInterpolation(InterpolatorUtil.InterpolatorType.SLERP);
                } else {
                    channel.content.interpolator = InterpolatorUtil.fromInterpolation(InterpolatorUtil.InterpolatorType.valueOf(interpolation.name()));
                }
                channel.node = nodeModel.getName();

                //init channel's keyframe time and keyframe values
                AccessorModel input = sampler.input();                 //accessor of key frame time
                AccessorData inputData = input.getAccessorData();
                if (!(inputData instanceof AccessorFloatData inputFloatData)) {
                    LogUtils.getLogger().warn(
                            "Input data is not an AccessorFloatData, but "
                                    + inputData.getClass());
                    return result;
                }
                AccessorModel output = sampler.output();               //accessor of key frame values
                AccessorData outputData = output.getAccessorData();
                if (!(outputData instanceof AccessorFloatData outputFloatData)) {
                    LogUtils.getLogger().warn(
                            "Output data is not an AccessorFloatData, but "
                                    + inputData.getClass());
                    return result;
                }
                int numKeyElements = inputFloatData.getNumElements();
                int numValuesElements = outputFloatData.getTotalNumComponents() / numKeyElements;
                float[] keyframeTimeS = new float[numKeyElements];
                float[][] values = new float[numKeyElements][numValuesElements];
                for (int i = 0; i < numKeyElements; i++) {
                    keyframeTimeS[i] = inputFloatData.get(i);
                    for (int j = 0; j < numValuesElements; j++) {
                        values[i][j] = outputFloatData.get(i * numValuesElements + j);
                    }
                }
                channel.content.keyframeTimeS = keyframeTimeS;
                channel.content.values = values;

                //compile the interpolator after everything loaded
                channel.content.interpolator.compile(channel);

                //add channel to animation
                animation.addChannel(channel);

                //add Animation Listeners to animation
                if (suppliers != null) {
                    for (AnimationListenerSupplier supplier : suppliers) {
                        AnimationListener listener = supplier.supplyListeners(channel.node, channel.type);
                        if (listener != null)
                            channel.addListener(listener);
                    }
                }
            }

            //add animation to result list
            result.add(animation);
        }

        return result;
    }

    protected void addChannel(ObjectAnimationChannel channel) {
        channels.compute(channel.node, (node, list) -> {
            if (list == null) list = new ArrayList<>();
            list.add(channel);
            return list;
        });

        if (channel.getEndTimeS() > maxEndTimeS)
            maxEndTimeS = channel.getEndTimeS();
    }

    protected Map<String, List<ObjectAnimationChannel>> getChannels() {
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
