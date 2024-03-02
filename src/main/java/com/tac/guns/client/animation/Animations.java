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
import java.util.ArrayList;
import java.util.List;

public class Animations {
    public static AnimationController createControllerFromGltf(@Nonnull AnimationStructure structure, @Nonnull AnimationListenerSupplier supplier) {
        return new AnimationController(createAnimationFromGltf(structure, (AnimationListenerSupplier[]) null), supplier);
    }

    protected static @Nonnull List<ObjectAnimation> createAnimationFromGltf(@Nonnull AnimationStructure structure, @Nullable AnimationListenerSupplier... suppliers) {
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
}
