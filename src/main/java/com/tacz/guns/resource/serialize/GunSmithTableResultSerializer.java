package com.tacz.guns.resource.serialize;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.serialization.JsonOps;
import com.tacz.guns.crafting.result.GunSmithTableResult;

import java.lang.reflect.Type;

public class GunSmithTableResultSerializer implements JsonDeserializer<GunSmithTableResult> {
    @Override
    public GunSmithTableResult deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        return GunSmithTableResult.CODEC.parse(JsonOps.INSTANCE, json)
            .getOrThrow(JsonParseException::new);
    }
}
