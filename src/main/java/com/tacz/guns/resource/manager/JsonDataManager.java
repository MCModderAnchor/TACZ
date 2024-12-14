package com.tacz.guns.resource.manager;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.tacz.guns.GunMod;
import com.tacz.guns.util.ResourceScanner;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * 通用数据管理器<br>
 * 从资源包/数据包中读取json文件并解析为数据
 * @param <T> 数据类型
 */
public class JsonDataManager<T> extends SimplePreparableReloadListener<Map<ResourceLocation, JsonElement>> {
    protected final Map<ResourceLocation, T> dataMap = Maps.newHashMap();

    private final Gson gson;
    private final Class<T> dataClass;
    private final Marker marker;

    private final FileToIdConverter fileToIdConverter;

    public JsonDataManager(Class<T> dataClass, Gson pGson, String directory, String marker) {
        this(dataClass, pGson, FileToIdConverter.json(directory), marker);
    }

    public JsonDataManager(Class<T> dataClass, Gson pGson, FileToIdConverter fileToIdConverter, String marker) {
        this.gson = pGson;
        this.dataClass = dataClass;
        this.marker = MarkerManager.getMarker(marker);
        this.fileToIdConverter = fileToIdConverter;
    }

    @NotNull
    @Override
    protected Map<ResourceLocation, JsonElement> prepare(ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        return ResourceScanner.scanDirectory(pResourceManager, fileToIdConverter, this.gson);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        dataMap.clear();
        for (Map.Entry<ResourceLocation, JsonElement> entry : pObject.entrySet()) {
            ResourceLocation id = entry.getKey();
            JsonElement element = entry.getValue();
            try {
                T data = parseJson(element);
                if (data != null) {
                    dataMap.put(id, data);
                }
            } catch (JsonParseException e) {
                GunMod.LOGGER.error(marker, "Failed to load data file {}", id, e);
            }
        }
    }

    protected T parseJson(JsonElement element) {
        return gson.fromJson(element, getDataClass());
    }

    public Class<T> getDataClass() {
        return dataClass;
    }

    public Marker getMarker() {
        return marker;
    }

    public Gson getGson() {
        return gson;
    }

    public T getData(ResourceLocation id) {
        return dataMap.get(id);
    }

    public Map<ResourceLocation, T> getAllData() {
        return dataMap;
    }
}
