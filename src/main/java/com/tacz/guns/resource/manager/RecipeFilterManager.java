package com.tacz.guns.resource.manager;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.tacz.guns.GunMod;
import com.tacz.guns.resource.CommonAssetsManager;
import com.tacz.guns.resource.filter.RecipeFilter;
import com.tacz.guns.resource.network.DataType;
import com.tacz.guns.util.ResourceScanner;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;


public class RecipeFilterManager extends SimplePreparableReloadListener<Map<ResourceLocation, List<JsonElement>>> implements INetworkCacheReloadListener {
    private final Map<ResourceLocation, RecipeFilter> filters = Maps.newHashMap();

    private final Gson gson;
    private final Marker marker;

    private final FileToIdConverter fileToIdConverter;
    protected Map<ResourceLocation, String> networkCache;

    public RecipeFilterManager() {
        this.gson = CommonAssetsManager.GSON;
        this.marker = MarkerManager.getMarker("RecipeFilter");
        this.fileToIdConverter = FileToIdConverter.json("recipe_filters");
    }

    @NotNull
    @Override
    protected Map<ResourceLocation, List<JsonElement>> prepare(ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        return ResourceScanner.scanDirectoryAll(pResourceManager, this.fileToIdConverter, this.gson);
    }

    @Override
    protected void apply(Map<ResourceLocation, List<JsonElement>> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        filters.clear();

        ImmutableMap.Builder<ResourceLocation, String> builder = ImmutableMap.builder();

        for (Map.Entry<ResourceLocation, List<JsonElement>> entry : pObject.entrySet()) {
            ResourceLocation id = entry.getKey();

            for (JsonElement element : entry.getValue()) {
                try {
                    RecipeFilter data = parseJson(element);
                    filters.compute(id, (key, value) -> {
                        if (value == null) {
                            return data;
                        } else {
                            value.merge(data);
                            return value;
                        }
                    });
                } catch (JsonParseException e) {
                    GunMod.LOGGER.error(marker, "Failed to load data file {}", id, e);
                }
            }

            if (filters.containsKey(id)) {
                builder.put(id, gson.toJson(filters.get(id)));
            }
        }

        this.networkCache = builder.build();
    }

    private RecipeFilter parseJson(JsonElement element) {
        return gson.fromJson(element, RecipeFilter.class);
    }

    @Override
    public Map<ResourceLocation, String> getNetworkCache() {
        return networkCache;
    }

    @Override
    public DataType getType() {
        return DataType.RECIPE_FILTER;
    }

    public Map<ResourceLocation, RecipeFilter> getFilters() {
        return filters;
    }

    public RecipeFilter getFilter(ResourceLocation id) {
        return filters.get(id);
    }
}
