package com.tacz.guns.client.resource.serialize;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.ParticleEffectData;
import com.google.gson.*;
import com.tacz.guns.client.resource.pojo.animation.bedrock.ParticleEffectKeyframes;
import it.unimi.dsi.fastutil.doubles.Double2ObjectRBTreeMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * 解析动画 JSON 中的 {@code "particle_effects"}：
 * <pre>
 * "particle_effects": {
 *     "0.0": { "effect": "tacz:muzzle_flash_rifle", "locator": "muzzle_flash" },
 *     "0.25": "tacz:reload_smoke"
 * }
 * </pre>
 * 值为字符串时仅指定效果 id，locator 留空（渲染时回退到枪口骨骼）。
 */
public class ParticleEffectKeyframesSerializer implements JsonDeserializer<ParticleEffectKeyframes> {
    @Override
    public ParticleEffectKeyframes deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        Double2ObjectRBTreeMap<ParticleEffectData> keyframes = new Double2ObjectRBTreeMap<>();
        if (json.isJsonObject()) {
            JsonObject jsonObject = json.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                double time;
                try {
                    time = Double.parseDouble(entry.getKey());
                } catch (NumberFormatException e) {
                    continue;
                }
                JsonElement value = entry.getValue();
                ResourceLocation effect = null;
                String locator = null;
                String preEffectScript = null;
                if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                    effect = ResourceLocation.tryParse(value.getAsString());
                } else if (value.isJsonObject()) {
                    JsonObject obj = value.getAsJsonObject();
                    effect = ResourceLocation.tryParse(GsonHelper.getAsString(obj, "effect"));
                    if (obj.has("locator")) {
                        locator = GsonHelper.getAsString(obj, "locator");
                    }
                    if (obj.has("pre_effect_script")) {
                        preEffectScript = GsonHelper.getAsString(obj, "pre_effect_script");
                    }
                }
                if (effect != null) {
                    keyframes.put(time, new ParticleEffectData(effect, locator, preEffectScript));
                }
            }
        }
        return new ParticleEffectKeyframes(keyframes);
    }
}
