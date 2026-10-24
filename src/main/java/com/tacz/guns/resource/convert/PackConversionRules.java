package com.tacz.guns.resource.convert;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Explicit, versioned item-tag rules. User entries override or extend the bundled defaults. */
public final class PackConversionRules {
    private static final String RESOURCE = "/data/tacz/gunpack_conversion_rules.json";
    private final Map<String, Rule> itemTags;
    private final String source;

    private PackConversionRules(Map<String, Rule> itemTags, String source) {
        this.itemTags = Map.copyOf(itemTags);
        this.source = source;
    }

    public static PackConversionRules defaults() {
        try (InputStream stream = PackConversionRules.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) throw new IllegalStateException("Missing bundled conversion rules: " + RESOURCE);
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return merge(null, JsonParser.parseReader(reader).getAsJsonObject(), "bundled");
            }
        } catch (IOException e) { throw new IllegalStateException("Cannot read bundled conversion rules", e); }
    }

    public static PackConversionRules load(Path override) throws IOException {
        PackConversionRules defaults = defaults();
        if (!Files.exists(override)) return defaults;
        try (Reader reader = Files.newBufferedReader(override, StandardCharsets.UTF_8)) {
            return merge(defaults, JsonParser.parseReader(reader).getAsJsonObject(), override.toString());
        }
    }

    public static PackConversionRules merge(PackConversionRules base, JsonObject json, String source) {
        if (!json.has("schema_version") || !json.get("schema_version").isJsonPrimitive()
            || !json.getAsJsonPrimitive("schema_version").isNumber() || json.get("schema_version").getAsDouble() != 1.0) {
            throw new IllegalArgumentException("Conversion rules require schema_version: 1");
        }
        if (json.has("source_minecraft") && !json.get("source_minecraft").getAsString().equals("1.20.1")) throw new IllegalArgumentException("Rules must target source_minecraft 1.20.1");
        if (json.has("target_minecraft") && !json.get("target_minecraft").getAsString().equals("1.21.1")) throw new IllegalArgumentException("Rules must target target_minecraft 1.21.1");
        Map<String, Rule> rules = new TreeMap<>();
        if (base != null) rules.putAll(base.itemTags);
        JsonObject entries = json.getAsJsonObject("item_tags");
        if (entries == null) throw new IllegalArgumentException("Conversion rules require item_tags");
        for (var entry : entries.entrySet()) {
            ResourceLocation.parse(entry.getKey());
            JsonObject value = entry.getValue().getAsJsonObject();
            String tag = string(value, "tag");
            String item = string(value, "item");
            String fallback = string(value, "fallback_item");
            List<String> allOf = new ArrayList<>();
            if (value.has("all_of")) for (JsonElement child : value.getAsJsonArray("all_of")) {
                if (!child.isJsonPrimitive() || !child.getAsJsonPrimitive().isString()) throw new IllegalArgumentException("all_of entries must be tag ID strings");
                allOf.add(child.getAsString());
            }
            if ((tag != null ? 1 : 0) + (item != null ? 1 : 0) + (!allOf.isEmpty() ? 1 : 0) != 1) {
                throw new IllegalArgumentException("Exactly one of tag/item/all_of is required: " + entry.getKey());
            }
            if (tag != null) ResourceLocation.parse(tag);
            if (item != null) ResourceLocation.parse(item);
            if (fallback != null) ResourceLocation.parse(fallback);
            for (String id : allOf) ResourceLocation.parse(id);
            rules.put(entry.getKey(), new Rule(tag, item, List.copyOf(allOf), fallback));
        }
        return new PackConversionRules(rules, source);
    }

    private static String string(JsonObject json, String name) {
        if (!json.has(name)) return null;
        JsonElement value = json.get(name);
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) throw new IllegalArgumentException(name + " must be an ID string");
        return value.getAsString();
    }
    public Rule get(String id) { return itemTags.get(id); }
    public int size() { return itemTags.size(); }
    public String source() { return source; }
    public Set<String> keys() { return itemTags.keySet(); }
    public record Rule(String tag, String item, List<String> allOf, String fallbackItem) { }
}
