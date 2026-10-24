package com.tacz.guns.resource.convert;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;

/** Checks target schemas without requiring optional mods to be installed during conversion. */
public final class ConversionValidation {
    private ConversionValidation() { }

    public static <T> void validate(Codec<T> codec, DynamicOps<JsonElement> ops, JsonElement json,
                                   PackConversionPlan report, String path) {
        codec.parse(ops, prepare(json, report, path)).getOrThrow(IllegalArgumentException::new);
    }

    /** The copy is used only by codecs; original identifiers always remain in the output. */
    public static JsonElement prepare(JsonElement json, PackConversionPlan report, String path) {
        if (json.isJsonArray()) {
            JsonArray copy = new JsonArray();
            for (int i = 0; i < json.getAsJsonArray().size(); i++) copy.add(prepare(json.getAsJsonArray().get(i), report, path + "[" + i + "]"));
            return copy;
        }
        if (!json.isJsonObject()) return json.deepCopy();
        JsonObject input = json.getAsJsonObject();
        JsonObject copy = new JsonObject();
        boolean stack = input.has("id") && !input.has("type")
            && (input.has("count") || input.has("components") || Set.of("id", "count", "components").containsAll(input.keySet()));
        boolean lootItem = input.has("type") && input.get("type").isJsonPrimitive() && input.get("type").getAsString().equals("minecraft:item");
        for (var entry : input.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();
            String childPath = path + "." + key;
            if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                && (key.equals("item") || key.equals("items") || (stack && key.equals("id")) || (lootItem && key.equals("name")))) {
                copy.addProperty(key, validationItem(value.getAsString(), report, childPath));
            } else if (key.equals("items") && value.isJsonArray()) {
                JsonArray items = new JsonArray();
                for (int i = 0; i < value.getAsJsonArray().size(); i++) {
                    JsonElement item = value.getAsJsonArray().get(i);
                    items.add(item.isJsonPrimitive() && item.getAsJsonPrimitive().isString()
                        ? new JsonPrimitive(validationItem(item.getAsString(), report, childPath + "[" + i + "]"))
                        : prepare(item, report, childPath + "[" + i + "]"));
                }
                copy.add(key, items);
            } else if (key.equals("minecraft:custom_data") || key.equals("nbt")) {
                copy.add(key, value.deepCopy());
            } else {
                copy.add(key, prepare(value, report, childPath));
            }
        }
        return copy;
    }

    private static String validationItem(String value, PackConversionPlan report, String path) {
        if (value.startsWith("#")) {
            ResourceLocation.parse(value.substring(1));
            return value;
        }
        ResourceLocation id = ResourceLocation.parse(value);
        if (BuiltInRegistries.ITEM.containsKey(id)) return value;
        if (report != null) report.deferItem(value, path);
        return "minecraft:stone";
    }
}
