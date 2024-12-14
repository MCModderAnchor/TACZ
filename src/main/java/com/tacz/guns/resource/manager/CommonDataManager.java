package com.tacz.guns.resource.manager;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.tacz.guns.resource.network.DataType;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/**
 * 服务端侧数据管理器<br>
 * 该类型的数据管理器用于服务端数据加载并向客户端同步
 * @param <T> 数据类型
 */
public class CommonDataManager<T> extends JsonDataManager<T> implements INetworkCacheReloadListener {
    private final DataType type;
    protected Map<ResourceLocation, String> networkCache;

    public CommonDataManager(DataType type, Class<T> dataClass, Gson pGson, String directory, String marker) {
        super(dataClass, pGson, directory, marker);
        this.type = type;
    }

    public CommonDataManager(DataType type, Class<T> dataClass, Gson pGson, FileToIdConverter fileToIdConverter, String marker) {
        super(dataClass, pGson, fileToIdConverter, marker);
        this.type = type;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        super.apply(pObject, pResourceManager, pProfiler);

        ImmutableMap.Builder<ResourceLocation, String> builder = ImmutableMap.builder();
        pObject.forEach((id, element) -> builder.put(id, element.toString()));
        this.networkCache = builder.build();
    }

    public void clear() {
        this.dataMap.clear();
    }

    public Map<ResourceLocation, String> getNetworkCache() {
        return this.networkCache;
    }

    public DataType getType() {
        return this.type;
    }
}
