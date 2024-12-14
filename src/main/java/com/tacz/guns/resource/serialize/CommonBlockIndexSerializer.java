package com.tacz.guns.resource.serialize;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.tacz.guns.resource.index.CommonBlockIndex;
import com.tacz.guns.resource.pojo.BlockIndexPOJO;

import java.lang.reflect.Type;

public class CommonBlockIndexSerializer implements JsonDeserializer<CommonBlockIndex> {
    @Override
    public CommonBlockIndex deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        try {
            BlockIndexPOJO pojo = context.deserialize(json, BlockIndexPOJO.class);
            return CommonBlockIndex.getInstance(pojo);
        } catch (IllegalArgumentException e) {
            throw new JsonParseException(e.getMessage());
        }
    }
}
