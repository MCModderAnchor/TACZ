package com.tac.guns.client.resource.cache;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.audio.SoundBuffer;
import com.tac.guns.client.animation.gltf.AnimationStructure;
import com.tac.guns.client.resource.pojo.data.GunData;
import com.tac.guns.client.resource.pojo.display.GunDisplay;
import com.tac.guns.client.resource.pojo.model.BedrockModelPOJO;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/**
 * 缓存 Map 的键统一为 ResourceLocation，其 namespace 为枪包的根目录的下一级文件夹的名称， path 为资源对应的 id 。
 * 举例来说，如果需要获取枪包中 "tac/index"
 */
public enum ClientAssetManager {
    INSTANCE;
    /**
     * 储存 display 数据
     */
    private final Map<ResourceLocation, GunDisplay> gunDisplays = Maps.newHashMap();
    /**
     * 储存 data 数据
     */
    private final Map<ResourceLocation, GunData> gunData = Maps.newHashMap();
    /**
     * 储存动画
     */
    private final Map<ResourceLocation, AnimationStructure> animations = Maps.newHashMap();
    /**
     * 储存模型
     */
    private final Map<ResourceLocation, BedrockModelPOJO> models = Maps.newHashMap();
    /**
     * 储存声音
     */
    private final Map<ResourceLocation, SoundBuffer> soundBuffers = Maps.newHashMap();

    public void putGunDisplay(ResourceLocation registryName, GunDisplay display) {
        gunDisplays.put(registryName, display);
    }

    public void putGunData(ResourceLocation registryName, GunData data) {
        gunData.put(registryName, data);
    }

    public void putAnimation(ResourceLocation registryName, AnimationStructure animation) {
        animations.put(registryName, animation);
    }

    public void putModel(ResourceLocation registryName, BedrockModelPOJO model) {
        models.put(registryName, model);
    }

    public void putSoundBuffer(ResourceLocation registryName, SoundBuffer soundBuffer) {
        soundBuffers.put(registryName, soundBuffer);
    }

    public GunDisplay getGunDisplay(ResourceLocation registryName) {
        return gunDisplays.get(registryName);
    }

    public GunData getGunData(ResourceLocation registryName) {
        return gunData.get(registryName);
    }

    public AnimationStructure getAnimations(ResourceLocation registryName) {
        return animations.get(registryName);
    }

    public BedrockModelPOJO getModels(ResourceLocation registryName) {
        return models.get(registryName);
    }

    public SoundBuffer getSoundBuffers(ResourceLocation registryName) {
        return soundBuffers.get(registryName);
    }

    /**
     * 清除所有缓存
     */
    public void clearAll() {
        // TODO：重载时清理缓存
    }
}
