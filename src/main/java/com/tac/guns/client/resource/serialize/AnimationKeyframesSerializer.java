package com.tac.guns.client.resource.serialize;

import com.google.gson.*;
import com.mojang.math.Vector3f;
import com.tac.guns.GunMod;
import com.tac.guns.client.resource.pojo.animation.bedrock.AnimationKeyframes;
import it.unimi.dsi.fastutil.doubles.Double2ObjectLinkedOpenHashMap;
import net.minecraft.util.GsonHelper;

import java.lang.reflect.Type;
import java.util.Map;

@SuppressWarnings("ALL")
public class AnimationKeyframesSerializer implements JsonDeserializer<AnimationKeyframes> {
    @Override
    public AnimationKeyframes deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        Double2ObjectLinkedOpenHashMap<AnimationKeyframes.Keyframe> keyframes = new Double2ObjectLinkedOpenHashMap<>();
        // 如果是数字
        if (json.isJsonPrimitive()) {
            if (json.getAsJsonPrimitive().isString()) {
                GunMod.LOGGER.warn("Molang is not supported: \"{}\"", json.getAsString());
                return new AnimationKeyframes(keyframes);
            } else {
                float value = json.getAsJsonPrimitive().getAsFloat();
                Vector3f data = new Vector3f(value, value, value);
                var keyframe = new AnimationKeyframes.Keyframe(null, null, data, null);
                keyframes.put(0, keyframe);
                return new AnimationKeyframes(keyframes);
            }
        }
        // 如果是数组
        if (json.isJsonArray()) {
            Vector3f data = this.readVector3f(json.getAsJsonArray());
            var keyframe = new AnimationKeyframes.Keyframe(null, null, data, null);
            keyframes.put(0, keyframe);
            return new AnimationKeyframes(keyframes);
        }
        // 如果是对象
        if (json.isJsonObject()) {
            JsonObject jsonObject = json.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entrySet : jsonObject.entrySet()) {
                double time = Double.parseDouble(entrySet.getKey());
                AnimationKeyframes.Keyframe keyframe = readKeyFrames(entrySet.getValue());
                keyframes.put(time, keyframe);
            }
            return new AnimationKeyframes(keyframes);
        }
        return new AnimationKeyframes(keyframes);
    }

    private AnimationKeyframes.Keyframe readKeyFrames(JsonElement element) {
        // 如果是数组
        if (element.isJsonArray()) {
            Vector3f data = this.readVector3f(element.getAsJsonArray());
            return new AnimationKeyframes.Keyframe(null, null, data, null);
        }
        // 如果是对象
        if (element.isJsonObject()) {
            JsonObject jsonObject = element.getAsJsonObject();
            String lerpMode = null;
            Vector3f pre = null;
            Vector3f post = null;
            if (jsonObject.has("lerp_mode")) {
                lerpMode = jsonObject.get("lerp_mode").getAsString();
            }
            if (jsonObject.has("pre") && jsonObject.get("pre").isJsonArray()) {
                JsonArray array = jsonObject.get("pre").getAsJsonArray();
                pre = this.readVector3f(array);
            }
            if (jsonObject.has("post") && jsonObject.get("post").isJsonArray()) {
                JsonArray array = jsonObject.get("post").getAsJsonArray();
                post = this.readVector3f(array);
            }
            return new AnimationKeyframes.Keyframe(pre, post, null, lerpMode);
        }
        return new AnimationKeyframes.Keyframe(null, null, null, null);
    }

    private Vector3f readVector3f(JsonArray array) {
        JsonElement xElement = array.get(0);
        JsonElement yElement = array.get(1);
        JsonElement zElement = array.get(2);
        float x = readVector3fElement(xElement, "(array i=0)");
        float y = readVector3fElement(xElement, "(array i=1)");
        float z = readVector3fElement(xElement, "(array i=2)");
        return new Vector3f(x, y, z);
    }

    private float readVector3fElement(JsonElement element, String memberName) {
        if (element.getAsJsonPrimitive().isString()) {
            GunMod.LOGGER.warn("Molang is not supported: \"{}\"", element.getAsString());
            return 0;
        } else {
            return GsonHelper.convertToFloat(element, memberName);
        }
    }
}
