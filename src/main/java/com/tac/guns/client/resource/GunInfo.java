package com.tac.guns.client.resource;

import com.google.common.collect.Maps;
import com.tac.guns.client.model.BedrockGunModel;
import com.tac.guns.client.resource.pojo.model.BedrockGunPOJO;

import java.util.Map;

public class GunInfo {
    private final Map<String, BedrockGunPOJO> infos = Maps.newHashMap();
    private final Map<String, BedrockGunModel> models = Maps.newHashMap();

    public void addInfo(String id, BedrockGunPOJO info, BedrockGunModel model) {
        infos.put(id, info);
        models.put(id, model);
    }

    public BedrockGunModel getGunModel(String id) {
        return models.get(id);
    }
}
