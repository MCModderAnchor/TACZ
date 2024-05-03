package com.tac.guns.client.resource.serialize;

import com.google.gson.*;
import com.tac.guns.client.resource.pojo.animation.bedrock.SoundEffectKeyframes;
import it.unimi.dsi.fastutil.doubles.Double2ObjectLinkedOpenHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

import java.lang.reflect.Type;
import java.util.Map;

@SuppressWarnings("ALL")
public class SoundEffectKeyframesSerializer implements JsonDeserializer<SoundEffectKeyframes> {
    @Override
    public SoundEffectKeyframes deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        Double2ObjectLinkedOpenHashMap<ResourceLocation> keyframes = new Double2ObjectLinkedOpenHashMap<>();
        // 如果是对象
        if (json.isJsonObject()) {
            JsonObject jsonObject = json.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entrySet : jsonObject.entrySet()) {
                double time = Double.parseDouble(entrySet.getKey());
                JsonElement value = entrySet.getValue();
                if (value.isJsonObject()) {
                    String soundId = GsonHelper.getAsString(value.getAsJsonObject(), "effect");
                    keyframes.put(time, new ResourceLocation(soundId));
                }
            }
            return new SoundEffectKeyframes(keyframes);
        }
        return new SoundEffectKeyframes(keyframes);
    }
}
