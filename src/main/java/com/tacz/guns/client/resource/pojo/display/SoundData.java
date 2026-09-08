package com.tacz.guns.client.resource.pojo.display;

import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;

public class SoundData {
    private final ResourceLocation sound;
    @Nullable
    private final Float volume;
    @Nullable
    private final Float pitch;
    private final float volumeRandomRange;
    private final float pitchRandomRange;

    private SoundData(ResourceLocation sound, @Nullable Float volume, @Nullable Float pitch, float volumeRandomRange, float pitchRandomRange) {
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
        this.volumeRandomRange = volumeRandomRange;
        this.pitchRandomRange = pitchRandomRange;
    }

    public static SoundData of(ResourceLocation sound) {
        return new SoundData(sound, null, null, 0, 0);
    }

    /**
     * 音效 id，同时也是 tacz_sounds 无对应音源时回退的原版 sound event id。
     */
    public ResourceLocation getSound() {
        return sound;
    }

    public boolean hasVolume() {
        return volume != null;
    }

    @Nullable
    public Float getVolume() {
        return volume;
    }

    public boolean hasPitch() {
        return pitch != null;
    }

    @Nullable
    public Float getPitch() {
        return pitch;
    }

    public float getVolumeRandomRange() {
        return volumeRandomRange;
    }

    public boolean hasVolumeRandomRange() {
        return volumeRandomRange > 0;
    }

    public float getPitchRandomRange() {
        return pitchRandomRange;
    }

    public boolean hasPitchRandomRange() {
        return pitchRandomRange > 0;
    }

    /**
     * 解析最终响度：未配置时使用传入的 fallback（保持原有行为），配置了则覆盖；有 volume_random_range 时在 ±range 内随机。
     */
    public float resolveVolume(RandomSource random, float fallback) {
        float base = hasVolume() ? volume : fallback;
        if (hasVolumeRandomRange()) {
            base += (random.nextFloat() * 2 - 1) * volumeRandomRange;
        }
        return Math.max(0, base);
    }

    /**
     * 解析最终音调：未配置时使用传入的 fallback（保持原有行为），配置了则覆盖；有 pitch_random_range 时在 ±range 内随机。
     */
    public float resolvePitch(RandomSource random, float fallback) {
        float base = hasPitch() ? pitch : fallback;
        if (hasPitchRandomRange()) {
            base += (random.nextFloat() * 2 - 1) * pitchRandomRange;
        }
        return Math.max(0.0001f, base);
    }

    @Override
    public String toString() {
        return "SoundData{sound=" + sound + ", volume=" + volume + ", pitch=" + pitch
                + ", volumeRandomRange=" + volumeRandomRange + ", pitchRandomRange=" + pitchRandomRange + '}';
    }

    /**
     * 兼容字符串（旧形式）与对象（新形式）两种 JSON 写法。
     */
    public static class Deserializer implements JsonDeserializer<SoundData> {
        @Override
        public SoundData deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            // 旧形式：字符串，直接作为音效 id / sound event id
            if (json.isJsonPrimitive()) {
                JsonPrimitive primitive = json.getAsJsonPrimitive();
                if (!primitive.isString()) {
                    throw new JsonParseException("Sound must be a string id or an object, got: " + json);
                }
                ResourceLocation sound = context.deserialize(primitive, ResourceLocation.class);
                if (sound == null) {
                    throw new JsonParseException("Sound id must not be null, got: " + json);
                }
                return SoundData.of(sound);
            }
            if (!json.isJsonObject()) {
                throw new JsonParseException("Sound must be a string id or an object, got: " + json);
            }
            JsonObject object = json.getAsJsonObject();
            JsonElement idElement = object.get("id");
            if (idElement == null || !idElement.isJsonPrimitive() || !idElement.getAsJsonPrimitive().isString()) {
                throw new JsonParseException("Sound object requires a string \"id\" field, got: " + json);
            }
            ResourceLocation sound = context.deserialize(idElement, ResourceLocation.class);
            if (sound == null) {
                throw new JsonParseException("Sound id must not be null, got: " + json);
            }

            Float volume = null;
            if (object.has("volume")) {
                float v = object.get("volume").getAsFloat();
                if (v < 0) {
                    throw new JsonParseException("Sound volume must be >= 0, got: " + json);
                }
                volume = v;
            }
            Float pitch = null;
            if (object.has("pitch")) {
                float p = object.get("pitch").getAsFloat();
                if (p <= 0) {
                    throw new JsonParseException("Sound pitch must be > 0, got: " + json);
                }
                pitch = p;
            }
            float volumeRandomRange = 0;
            if (object.has("volume_random_range")) {
                volumeRandomRange = object.get("volume_random_range").getAsFloat();
                if (volumeRandomRange < 0) {
                    throw new JsonParseException("Sound volume_random_range must be >= 0, got: " + json);
                }
            }
            float pitchRandomRange = 0;
            if (object.has("pitch_random_range")) {
                pitchRandomRange = object.get("pitch_random_range").getAsFloat();
                if (pitchRandomRange < 0) {
                    throw new JsonParseException("Sound pitch_random_range must be >= 0, got: " + json);
                }
            }
            return new SoundData(sound, volume, pitch, volumeRandomRange, pitchRandomRange);
        }
    }
}