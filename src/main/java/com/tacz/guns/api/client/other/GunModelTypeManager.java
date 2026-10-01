package com.tacz.guns.api.client.other;

import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.resource.pojo.model.BedrockModelPOJO;
import com.tacz.guns.client.resource.pojo.model.BedrockVersion;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class GunModelTypeManager {
    public static final Map<String, BiFunction<BedrockModelPOJO, BedrockVersion, ? extends BedrockGunModel>> GUN_MODEL_TYPE_MAP = new HashMap<>();

    //注册字符串对应的模型实例构造器到Map中
    //注意多线程安全
    public static synchronized void registerModelType(String typeName, BiFunction<BedrockModelPOJO, BedrockVersion, ? extends BedrockGunModel> constructor) {
        GUN_MODEL_TYPE_MAP.put(typeName, constructor);
    }

    //如果该字符串查询不到对应的模型实例的构造器，则返回默认构造器
    public static synchronized BiFunction<BedrockModelPOJO, BedrockVersion, ? extends BedrockGunModel> getModelInstanceConstructor(String typeName) {
        return GUN_MODEL_TYPE_MAP.getOrDefault(typeName, BedrockGunModel::new);
    }
}
