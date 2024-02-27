package com.tac.guns.client.resource;

import com.google.common.collect.Maps;
import com.tac.guns.client.animation.gltf.AnimationStructure;
import com.tac.guns.client.model.BedrockAnimatedModel;
import com.tac.guns.client.resource.pojo.model.BedrockGunPOJO;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

// todo 补充注释
public enum BedrockAssetManager {
    INSTANCE;

    private final Map<ResourceLocation, BedrockAnimatedModel> models = Maps.newHashMap();
    private final Map<ResourceLocation, AnimationStructure> animations = Maps.newHashMap();
    private final Map<ResourceLocation, BedrockGunPOJO> infos = Maps.newHashMap();

    public void addBedrockGunInfo(ResourceLocation registryName, BedrockGunPOJO info) {
        infos.put(registryName, info);
    }

    public BedrockGunPOJO getBedrockGunInfo(ResourceLocation registryName){
        return infos.get(registryName);
    }

    public void addModel(ResourceLocation registryName, BedrockAnimatedModel model){
        models.put(registryName, model);
    }

    public BedrockAnimatedModel getModel(ResourceLocation registryName){
        return models.get(registryName);
    }

    public void addAnimation(ResourceLocation registryName, AnimationStructure animation){
        animations.put(registryName, animation);
    }

    public AnimationStructure getAnimation(ResourceLocation registryName){
        return animations.get(registryName);
    }
}
