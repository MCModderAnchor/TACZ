package com.tacz.guns.resource.serialize;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.pojo.GunIndexPOJO;

import java.lang.reflect.Type;

public class CommonGunIndexSerializer implements JsonDeserializer<CommonGunIndex> {
    @Override
    public CommonGunIndex deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        try {
            GunIndexPOJO pojo = context.deserialize(json, GunIndexPOJO.class);
            return CommonGunIndex.getInstance(pojo);
        } catch (IllegalArgumentException e) {
            throw new JsonParseException(e.getMessage());
        }
    }
}
