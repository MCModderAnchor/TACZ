package com.tac.guns.client.resource.cache;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.audio.SoundBuffer;
import com.tac.guns.client.animation.AnimationController;
import com.tac.guns.client.animation.Animations;
import com.tac.guns.client.animation.gltf.AnimationStructure;
import com.tac.guns.client.model.BedrockAnimatedModel;
import com.tac.guns.client.resource.cache.data.BedrockAnimatedAsset;
import com.tac.guns.client.resource.cache.data.ClientGunIndex;
import com.tac.guns.client.resource.pojo.animation.gltf.RawAnimationStructure;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/**
 * 缓存 Map 的键统一为 ResourceLocation，其 namespace 为枪包的根目录的下一级文件夹的名称， path 为资源对应的 id 。
 * 举例来说，如果需要获取枪包中 "tac/index"
 */
public enum ClientAssetManager {
    INSTANCE;
    /**
     * 储存客户端需要的其他枪械数据，如显示名、tooltip、后坐力大小等
     */
    private final Map<ResourceLocation, ClientGunIndex> indexes = Maps.newHashMap();
    /**
     * 储存动画和模型，包括 模型、与模型绑定的动画控制器 等等。
     */
    private final Map<ResourceLocation, BedrockAnimatedAsset> animatedAssets = Maps.newHashMap();
    /**
     * 储存枪械需要的所有声音的注册名。
     */
    private final Map<ResourceLocation, SoundBuffer> soundBuffers = Maps.newHashMap();

    public void putGunIndex(ResourceLocation registryName, ClientGunIndex index) {
        indexes.put(registryName, index);
    }

    public void putBedrockAnimatedAsset(ResourceLocation registryName, BedrockAnimatedModel model, RawAnimationStructure animation) {
        AnimationController controller = Animations.createControllerFromGltf(new AnimationStructure(animation), model);
        animatedAssets.put(registryName, new BedrockAnimatedAsset(model, controller));
    }

    public void putSoundBuffer(ResourceLocation registryName, SoundBuffer soundBuffer) {
        soundBuffers.put(registryName, soundBuffer);
    }

    public ClientGunIndex getGunIndex(ResourceLocation registryName) {
        return indexes.get(registryName);
    }

    public BedrockAnimatedAsset getBedrockAnimatedAsset(ResourceLocation registryName) {
        return animatedAssets.get(registryName);
    }

    public SoundBuffer getSoundBuffers(ResourceLocation registryName) {
        return soundBuffers.get(registryName);
    }
}
