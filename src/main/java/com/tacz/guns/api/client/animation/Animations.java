package com.tacz.guns.api.client.animation;

import com.tacz.guns.api.client.animation.gltf.AccessorModel;
import com.tacz.guns.api.client.animation.gltf.AnimationModel;
import com.tacz.guns.api.client.animation.gltf.AnimationStructure;
import com.tacz.guns.api.client.animation.gltf.NodeModel;
import com.tacz.guns.api.client.animation.gltf.accessor.AccessorData;
import com.tacz.guns.api.client.animation.gltf.accessor.AccessorFloatData;
import com.tacz.guns.api.client.animation.interpolator.CustomInterpolator;
import com.tacz.guns.api.client.animation.interpolator.InterpolatorUtil;
import com.tacz.guns.client.resource.pojo.animation.bedrock.*;
import com.tacz.guns.util.math.MathUtil;
import it.unimi.dsi.fastutil.doubles.Double2ObjectMap;
import it.unimi.dsi.fastutil.doubles.Double2ObjectRBTreeMap;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Animations {
    public static AnimationController createControllerFromGltf(@Nonnull AnimationStructure structure, @Nonnull AnimationListenerSupplier supplier) {
        List<ObjectAnimation> prototypes = new ArrayList<>();

        List<AnimationModel> animationModels = structure.getAnimationModels();
        for (AnimationModel animationModel : animationModels) {
            ObjectAnimation animation = new ObjectAnimation(animationModel.getName());

            // 初始化动画轨道
            List<AnimationModel.Channel> channelModels = animationModel.getChannels();
            for (AnimationModel.Channel channelModel : channelModels) {
                ObjectAnimationChannel channel = new ObjectAnimationChannel(ObjectAnimationChannel.ChannelType.valueOf(channelModel.path().toUpperCase(Locale.ENGLISH)));
                AnimationModel.Sampler sampler = channelModel.sampler();

                // 初始化轨道的节点名称和插值器
                AnimationModel.Interpolation interpolation = sampler.interpolation();
                NodeModel nodeModel = channelModel.nodeModel();
                // 四元数需要特殊的插值
                if (channel.type.equals(ObjectAnimationChannel.ChannelType.ROTATION) && interpolation.equals(AnimationModel.Interpolation.LINEAR)) {
                    channel.interpolator = InterpolatorUtil.fromInterpolation(InterpolatorUtil.InterpolatorType.SLERP);
                } else {
                    channel.interpolator = InterpolatorUtil.fromInterpolation(InterpolatorUtil.InterpolatorType.valueOf(interpolation.name()));
                }
                channel.node = nodeModel.getName();

                // 计算出各个初始值的逆
                AnimationListener animationListener = supplier.supplyListeners(channel.node, channel.type);
                if (animationListener == null) {
                    continue;
                }
                float[] inverseValue = animationListener.initialValue();
                if (channel.type == ObjectAnimationChannel.ChannelType.ROTATION) {
                    if (inverseValue.length == 3) {
                        inverseValue = MathUtil.toQuaternion(inverseValue[0], inverseValue[1], inverseValue[2]);
                    }
                    inverseValue = MathUtil.inverseQuaternion(inverseValue);
                } else if (channel.type == ObjectAnimationChannel.ChannelType.TRANSLATION) {
                    inverseValue[0] = -inverseValue[0];
                    inverseValue[1] = -inverseValue[1];
                    inverseValue[2] = -inverseValue[2];
                }

                // 初始化轨道的关键帧时间和关键帧数值
                // 关键帧时间的访问器
                AccessorModel input = sampler.input();
                AccessorData inputData = input.getAccessorData();
                if (!(inputData instanceof AccessorFloatData inputFloatData)) {
                    throw new IllegalArgumentException("Input data is not an AccessorFloatData, but " + inputData.getClass());
                }
                // 关键帧时间的访问器
                AccessorModel output = sampler.output();
                AccessorData outputData = output.getAccessorData();
                if (!(outputData instanceof AccessorFloatData outputFloatData)) {
                    throw new IllegalArgumentException("Output data is not an AccessorFloatData, but " + inputData.getClass());
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
                    if (channel.type == ObjectAnimationChannel.ChannelType.ROTATION) {
                        values[i] = MathUtil.toEulerAngles(values[i]);
                        values[i] = MathUtil.toQuaternion(-values[i][0], -values[i][1], values[i][2]);
                        values[i] = MathUtil.mulQuaternion(inverseValue, values[i]);
                    } else if (channel.type == ObjectAnimationChannel.ChannelType.TRANSLATION) {
                        values[i][0] = -values[i][0] + inverseValue[0];
                        values[i][1] = -(-values[i][1] + inverseValue[1]);
                        values[i][2] = values[i][2] + inverseValue[2];
                    }
                }
                channel.content.keyframeTimeS = keyframeTimeS;
                channel.content.values = values;

                // 加载完所有内容后编译插值器
                channel.interpolator.compile(channel.content);

                // 将轨道添加到动画
                animation.addChannel(channel);
            }

            // 将动画添加到原型列表中
            prototypes.add(animation);
        }
        return new AnimationController(prototypes, supplier);
    }

    public static AnimationController createControllerFromBedrock(BedrockAnimationFile animationFile, AnimationListenerSupplier supplier) {
        return new AnimationController(createAnimationFromBedrock(animationFile), supplier);
    }

    public static @Nonnull List<ObjectAnimation> createAnimationFromBedrock(BedrockAnimationFile animationFile) {
        List<ObjectAnimation> result = new ArrayList<>();
        for (Map.Entry<String, BedrockAnimation> animationEntry : animationFile.getAnimations().entrySet()) {
            ObjectAnimation animation = new ObjectAnimation(animationEntry.getKey());
            BedrockAnimation bedrockAnimation = animationEntry.getValue();
            if (bedrockAnimation.getBones() != null) {
                for (Map.Entry<String, AnimationBone> boneEntry : bedrockAnimation.getBones().entrySet()) {
                    AnimationBone bone = boneEntry.getValue();
                    AnimationKeyframes translationKeyframes = bone.getPosition();
                    AnimationKeyframes rotationKeyframes = bone.getRotation();
                    AnimationKeyframes scaleKeyframes = bone.getScale();
                    if (translationKeyframes != null) {
                        ObjectAnimationChannel translationChannel = new ObjectAnimationChannel(ObjectAnimationChannel.ChannelType.TRANSLATION);
                        translationChannel.node = boneEntry.getKey();
                        translationChannel.interpolator = new CustomInterpolator();
                        // 将位移数据转移进 AnimationChannel
                        writeBedrockTranslation(translationChannel, bone.getPosition());
                        translationChannel.interpolator.compile(translationChannel.content);
                        animation.addChannel(translationChannel);
                    }
                    if (rotationKeyframes != null) {
                        ObjectAnimationChannel rotationChannel = new ObjectAnimationChannel(ObjectAnimationChannel.ChannelType.ROTATION);
                        rotationChannel.node = boneEntry.getKey();
                        rotationChannel.interpolator = new CustomInterpolator();
                        // 将旋转数据转移进 AnimationChannel
                        writeBedrockRotation(rotationChannel, bone.getRotation());
                        rotationChannel.interpolator.compile(rotationChannel.content);
                        animation.addChannel(rotationChannel);
                    }
                    if (scaleKeyframes != null) {
                        ObjectAnimationChannel scaleChannel = new ObjectAnimationChannel(ObjectAnimationChannel.ChannelType.SCALE);
                        scaleChannel.node = boneEntry.getKey();
                        scaleChannel.interpolator = new CustomInterpolator();
                        // 将缩放数据转移进 AnimationChannel
                        writeBedrockScale(scaleChannel, bone.getScale());
                        scaleChannel.interpolator.compile(scaleChannel.content);
                        animation.addChannel(scaleChannel);
                    }
                }
            }
            // 将声音数据转移到 ObjectAnimation 中
            SoundEffectKeyframes soundEffectKeyframes = bedrockAnimation.getSoundEffects();
            if (soundEffectKeyframes != null) {
                ObjectAnimationSoundChannel soundChannel = new ObjectAnimationSoundChannel();
                soundChannel.content = new AnimationSoundChannelContent();
                int keyframeNum = soundEffectKeyframes.getKeyframes().size();
                soundChannel.content.keyframeTimeS = new double[keyframeNum];
                soundChannel.content.keyframeSoundName = new ResourceLocation[keyframeNum];
                int i = 0;
                for (Map.Entry<Double, ResourceLocation> entry : soundEffectKeyframes.getKeyframes().double2ObjectEntrySet()) {
                    soundChannel.content.keyframeTimeS[i] = entry.getKey();
                    soundChannel.content.keyframeSoundName[i] = entry.getValue();
                    i++;
                }
                animation.setSoundChannel(soundChannel);
            }
            result.add(animation);
        }
        return result;
    }

    private static void writeBedrockTranslation(ObjectAnimationChannel animationChannel, AnimationKeyframes keyframes) {
        // 基岩版动画中储存的动画数据为相对值，而 tac 的动画系统使用的是绝对值，所以需要叠加初始值。
        // 此处就是在获取动画数据的初始值。
        Double2ObjectRBTreeMap<AnimationKeyframes.Keyframe> keyframesMap = keyframes.getKeyframes();
        animationChannel.content.keyframeTimeS = new float[keyframesMap.size()];
        animationChannel.content.values = new float[keyframesMap.size()][];
        animationChannel.content.lerpModes = new AnimationChannelContent.LerpMode[keyframesMap.size()];
        int index = 0;
        for (Double2ObjectMap.Entry<AnimationKeyframes.Keyframe> entry : keyframesMap.double2ObjectEntrySet()) {
            // 写入关键帧时间
            animationChannel.content.keyframeTimeS[index] = (float) entry.getDoubleKey();
            // 写入关键帧数值。
            AnimationKeyframes.Keyframe keyframe = entry.getValue();
            if (keyframe.pre() != null || keyframe.post() != null) {
                if (keyframe.pre() != null && keyframe.post() != null) {
                    animationChannel.content.values[index] = new float[6];
                    Vector3f pre = new Vector3f(keyframe.pre());
                    Vector3f post = new Vector3f(keyframe.post());
                    pre.mul(1 / 16f, 1 / 16f, 1 / 16f);
                    post.mul(1 / 16f, 1 / 16f, 1 / 16f);
                    readVector3fToArray(animationChannel.content.values[index], pre, 0);
                    readVector3fToArray(animationChannel.content.values[index], post, 3);
                } else if (keyframe.pre() != null) {
                    animationChannel.content.values[index] = new float[3];
                    Vector3f pre = new Vector3f(keyframe.pre());
                    pre.mul(1 / 16f, 1 / 16f, 1 / 16f);
                    readVector3fToArray(animationChannel.content.values[index], pre, 0);
                } else {
                    animationChannel.content.values[index] = new float[3];
                    Vector3f post = new Vector3f(keyframe.post());
                    post.mul(1 / 16f, 1 / 16f, 1 / 16f);
                    readVector3fToArray(animationChannel.content.values[index], post, 0);
                }
            } else if (keyframe.data() != null) {
                animationChannel.content.values[index] = new float[3];
                Vector3f data = new Vector3f(keyframe.data());
                data.mul(1 / 16f, 1 / 16f, 1 / 16f);
                readVector3fToArray(animationChannel.content.values[index], data, 0);
            }
            // 写入关键帧插值类型
            String lerpModeName = keyframe.lerpMode();
            if (lerpModeName != null) {
                try {
                    animationChannel.content.lerpModes[index] = AnimationChannelContent.LerpMode.valueOf(lerpModeName.toUpperCase(Locale.ENGLISH));
                } catch (IllegalArgumentException e) {
                    animationChannel.content.lerpModes[index] = AnimationChannelContent.LerpMode.LINEAR;
                }
            } else {
                animationChannel.content.lerpModes[index] = AnimationChannelContent.LerpMode.LINEAR;
            }
            index++;
        }
    }

    private static void writeBedrockRotation(ObjectAnimationChannel animationChannel, AnimationKeyframes keyframes) {
        Double2ObjectRBTreeMap<AnimationKeyframes.Keyframe> keyframesMap = keyframes.getKeyframes();
        animationChannel.content.keyframeTimeS = new float[keyframesMap.size()];
        animationChannel.content.values = new float[keyframesMap.size()][];
        animationChannel.content.lerpModes = new AnimationChannelContent.LerpMode[keyframesMap.size()];
        int index = 0;
        for (Double2ObjectMap.Entry<AnimationKeyframes.Keyframe> entry : keyframesMap.double2ObjectEntrySet()) {
            // 写入关键帧时间
            animationChannel.content.keyframeTimeS[index] = (float) entry.getDoubleKey();
            // 写入关键帧数值。
            AnimationKeyframes.Keyframe keyframe = entry.getValue();
            if (keyframe.pre() != null || keyframe.post() != null) {
                if (keyframe.pre() != null && keyframe.post() != null) {
                    animationChannel.content.values[index] = new float[6];
                    Vector3f pre = new Vector3f(keyframe.pre());
                    Vector3f post = new Vector3f(keyframe.post());
                    toAngle(pre);
                    toAngle(post);
                    animationChannel.content.values[index][0] = pre.x();
                    animationChannel.content.values[index][1] = pre.y();
                    animationChannel.content.values[index][2] = pre.z();
                    animationChannel.content.values[index][3] = post.x();
                    animationChannel.content.values[index][4] = post.y();
                    animationChannel.content.values[index][5] = post.z();
                } else if (keyframe.pre() != null) {
                    animationChannel.content.values[index] = new float[3];
                    Vector3f pre =  new Vector3f(keyframe.pre());
                    toAngle(pre);
                    animationChannel.content.values[index][0] = pre.x();
                    animationChannel.content.values[index][1] = pre.y();
                    animationChannel.content.values[index][2] = pre.z();
                } else {
                    animationChannel.content.values[index] = new float[3];
                    Vector3f post =  new Vector3f(keyframe.post());
                    toAngle(post);
                    animationChannel.content.values[index][0] = post.x();
                    animationChannel.content.values[index][1] = post.y();
                    animationChannel.content.values[index][2] = post.z();
                }
            } else if (keyframe.data() != null) {
                animationChannel.content.values[index] = new float[3];
                Vector3f data =  new Vector3f(keyframe.data());
                toAngle(data);
                animationChannel.content.values[index][0] = data.x();
                animationChannel.content.values[index][1] = data.y();
                animationChannel.content.values[index][2] = data.z();
            }
            String lerpModeName = keyframe.lerpMode();
            if (lerpModeName != null) {
                if (lerpModeName.equals(AnimationChannelContent.LerpMode.CATMULLROM.name().toLowerCase())) {
                    animationChannel.content.lerpModes[index] = AnimationChannelContent.LerpMode.CATMULLROM;
                } else {
                    animationChannel.content.lerpModes[index] = AnimationChannelContent.LerpMode.LINEAR;
                }
            } else {
                animationChannel.content.lerpModes[index] = AnimationChannelContent.LerpMode.LINEAR;
            }
            index++;
        }
    }

    private static void writeBedrockScale(ObjectAnimationChannel animationChannel, AnimationKeyframes keyframes) {
        Double2ObjectRBTreeMap<AnimationKeyframes.Keyframe> keyframesMap = keyframes.getKeyframes();
        animationChannel.content.keyframeTimeS = new float[keyframesMap.size()];
        animationChannel.content.values = new float[keyframesMap.size()][];
        animationChannel.content.lerpModes = new AnimationChannelContent.LerpMode[keyframesMap.size()];
        int index = 0;
        for (Double2ObjectMap.Entry<AnimationKeyframes.Keyframe> entry : keyframesMap.double2ObjectEntrySet()) {
            // 写入关键帧时间
            animationChannel.content.keyframeTimeS[index] = (float) entry.getDoubleKey();
            // 写入关键帧数值。
            AnimationKeyframes.Keyframe keyframe = entry.getValue();
            if (keyframe.pre() != null || keyframe.post() != null) {
                if (keyframe.pre() != null && keyframe.post() != null) {
                    animationChannel.content.values[index] = new float[6];
                    Vector3f pre = keyframe.pre();
                    Vector3f post = keyframe.post();
                    readVector3fToArray(animationChannel.content.values[index], pre, 0);
                    readVector3fToArray(animationChannel.content.values[index], post, 3);
                } else if (keyframe.pre() != null) {
                    animationChannel.content.values[index] = new float[3];
                    Vector3f pre = keyframe.pre();
                    readVector3fToArray(animationChannel.content.values[index], pre, 0);
                } else {
                    animationChannel.content.values[index] = new float[3];
                    Vector3f post = keyframe.post();
                    readVector3fToArray(animationChannel.content.values[index], post, 0);
                }
            } else if (keyframe.data() != null) {
                animationChannel.content.values[index] = new float[3];
                Vector3f data = keyframe.data();
                readVector3fToArray(animationChannel.content.values[index], data, 0);
            }
            // 写入关键帧插值类型
            String lerpModeName = keyframe.lerpMode();
            if (lerpModeName != null) {
                try {
                    animationChannel.content.lerpModes[index] = AnimationChannelContent.LerpMode.valueOf(lerpModeName.toUpperCase(Locale.ENGLISH));
                } catch (IllegalArgumentException e) {
                    animationChannel.content.lerpModes[index] = AnimationChannelContent.LerpMode.LINEAR;
                }
            } else {
                animationChannel.content.lerpModes[index] = AnimationChannelContent.LerpMode.LINEAR;
            }
            index++;
        }
    }

    private static void toAngle(Vector3f vector3f) {
        vector3f.set((float) Math.toRadians(vector3f.x()), (float) Math.toRadians(vector3f.y()), (float) Math.toRadians(vector3f.z()));
    }

    private static void readVector3fToArray(float[] array, Vector3f vector3f, int offset) {
        array[offset] = vector3f.x();
        array[offset + 1] = vector3f.y();
        array[offset + 2] = vector3f.z();
    }
}
