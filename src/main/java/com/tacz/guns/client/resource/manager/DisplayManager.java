package com.tacz.guns.client.resource.manager;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.tacz.guns.GunMod;
import com.tacz.guns.client.resource.pojo.display.IDisplay;
import com.tacz.guns.resource.manager.JsonDataManager;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/**
 * 通用数据管理器<br>
 * 从资源包/数据包中读取json文件并解析为数据
 * @param <T> 数据类型
 */
public class DisplayManager<T extends IDisplay> extends JsonDataManager<T> {

    public DisplayManager(Class<T> dataClass, Gson pGson, String directory, String marker) {
        super(dataClass, pGson, FileToIdConverter.json(directory), marker);
    }

    public DisplayManager(Class<T> dataClass, Gson pGson, FileToIdConverter fileToIdConverter, String marker) {
        super(dataClass, pGson, fileToIdConverter, marker);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        dataMap.clear();
        for (Map.Entry<ResourceLocation, JsonElement> entry : pObject.entrySet()) {
            ResourceLocation id = entry.getKey();
            JsonElement element = entry.getValue();
            try {
                T data = getGson().fromJson(element, getDataClass());
                if (data != null) {
                    data.init();
                    dataMap.put(id, data);
                }
            } catch (JsonParseException e) {
                GunMod.LOGGER.error(getMarker(), "Failed to load data file {}", id, e);
            }
        }
    }

}
