package com.tac.guns.client.resource;

import com.google.common.collect.Maps;
import com.tac.guns.client.animation.gltf.AnimationStructure;
import com.tac.guns.client.model.BedrockGunModel;
import com.tac.guns.client.resource.pojo.model.BedrockGunPOJO;

import java.util.Map;

public class GunInfo {
    private final Map<String, BedrockGunPOJO> infos = Maps.newHashMap();
    private final Map<String, BedrockGunModel> models = Maps.newHashMap();
    private final Map<String, AnimationStructure> animations = Maps.newHashMap();

    public void addInfo(String id, BedrockGunPOJO info, BedrockGunModel model) {
        infos.put(id, info);
        models.put(id, model);
    }

    public void addAnimation(String id, AnimationStructure structure){
        animations.put(id, structure);
    }

    public BedrockGunModel getGunModel(String id) {
        return models.get(id);
    }

    public AnimationStructure getAnimation(String id){
        return animations.get(id);
    }
}
