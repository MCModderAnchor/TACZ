package com.tacz.guns.resource.filter;

import com.google.gson.*;
import com.tacz.guns.GunMod;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.PatternSyntaxException;

public class RecipeFilter {
    public List<IFilter<ResourceLocation>> whitelist = new ArrayList<>();
    public List<IFilter<ResourceLocation>> blacklist = new ArrayList<>();

    public List<ResourceLocation> cache;

    public void merge(RecipeFilter other) {
        this.whitelist.addAll(other.whitelist);
        this.blacklist.addAll(other.blacklist);
    }

    public List<ResourceLocation> filter(List<ResourceLocation> input) {
        List<ResourceLocation> output = new ArrayList<>();
        for (ResourceLocation location : input) {
            boolean allowed = false;
            for (IFilter<ResourceLocation> filter : this.whitelist) {
                if (filter.test(location)) {
                    allowed = true;
                    break;
                }
            }
            for (IFilter<ResourceLocation> filter : this.blacklist) {
                if (filter.test(location)) {
                    allowed = false;
                    break;
                }
            }
            if (allowed) {
                output.add(location);
            }
        }
        return output;
    }

    public List<ResourceLocation> cache(List<ResourceLocation> input) {
        this.cache = this.filter(input);
        return this.cache;
    }

    public List<ResourceLocation> getCached() {
        return this.cache;
    }

    public static class Deserializer implements JsonDeserializer<RecipeFilter> {
        @Override
        public RecipeFilter deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            RecipeFilter filter = new RecipeFilter();
            JsonArray wl = json.getAsJsonObject().getAsJsonArray("whitelist");
            if (wl != null) {
                loadFilters(wl, filter.whitelist);
            }
            JsonArray bl = json.getAsJsonObject().getAsJsonArray("blacklist");
            if (bl != null) {
                loadFilters(bl, filter.blacklist);
            }
            return filter;
        }

        private void loadFilters(JsonArray array, @NotNull List<IFilter<ResourceLocation>> list) {
            LiteralFilter.Builder<ResourceLocation> builder = new LiteralFilter.Builder<>();
            for (JsonElement element : array) {
                if (element.isJsonPrimitive()) {
                    String entry = element.getAsString();
                    if (entry.startsWith("^")) {
                        try {
                            list.add(new RegexFilter<>(entry));
                        } catch (PatternSyntaxException e) {
                            GunMod.LOGGER.error("Failed to parse regex filter: {}", entry, e);
                        }
                    } else {
                        ResourceLocation rl = ResourceLocation.tryParse(entry);
                        if (rl != null){
                            builder.add(rl);
                        }
                    }
                } else {
                    throw new JsonParseException("Invalid recipe filter entry: " + element);
                }
            }
            list.add(builder.build());
        }
    }
}
