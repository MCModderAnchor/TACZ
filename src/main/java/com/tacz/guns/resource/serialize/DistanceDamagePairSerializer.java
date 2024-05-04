package com.tacz.guns.resource.serialize;

import com.google.gson.*;
import com.tacz.guns.resource.pojo.data.gun.ExtraDamage;

import java.lang.reflect.Type;

public class DistanceDamagePairSerializer implements JsonDeserializer<ExtraDamage.DistanceDamagePair> {
    private static final String INFINITE = "infinite";

    @Override
    public ExtraDamage.DistanceDamagePair deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (!json.isJsonObject()) {
            throw new JsonSyntaxException("Expected " + json + " to be a DistanceDamagePair because it's not an object");
        }
        JsonObject jsonObject = json.getAsJsonObject();
        if (!jsonObject.has("distance")) {
            throw new JsonSyntaxException("Expected " + json + " to be a DistanceDamagePair because it's not has distance field");
        }
        if (!jsonObject.has("damage")) {
            throw new JsonSyntaxException("Expected " + json + " to be a DistanceDamagePair because it's not has damage field");
        }
        if (!jsonObject.get("distance").isJsonPrimitive()) {
            throw new JsonSyntaxException("Expected " + json + " to be a DistanceDamagePair because it distance field is not a string or number");
        }
        if (!jsonObject.get("damage").isJsonPrimitive()) {
            throw new JsonSyntaxException("Expected " + json + " to be a DistanceDamagePair because it damage field is not a number");
        }
        float distance = 0;
        float damage;
        JsonPrimitive jsonPrimitive = jsonObject.get("distance").getAsJsonPrimitive();
        if (jsonPrimitive.isNumber()) {
            distance = jsonPrimitive.getAsFloat();
        } else if (jsonPrimitive.isString()) {
            if (INFINITE.equals(jsonPrimitive.getAsString())) {
                distance = Float.MAX_VALUE;
            } else {
                throw new JsonSyntaxException("Expected " + json + " to be a DistanceDamagePair because it distance field is not is '" + INFINITE + "'");
            }
        }
        damage = jsonObject.get("damage").getAsFloat();
        return new ExtraDamage.DistanceDamagePair(distance, damage);
    }
}
