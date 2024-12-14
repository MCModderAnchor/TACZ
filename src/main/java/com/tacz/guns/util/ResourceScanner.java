package com.tacz.guns.util;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.tacz.guns.GunMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

public class ResourceScanner {
    /**
     * 扫描指定目录下的所有json文件<br>
     * 与原版的scanDirectory方法的区别在于，查询结果是作为返回值返回的，而且允许注释
     * 对于相同的文件路径，只读取优先级最高的文件
     * @param pResourceManager 资源管理器
     * @param pName 目录名
     * @param pGson Gson实例
     * @return 扫描到的json文件
     */
    public static Map<ResourceLocation, JsonElement> scanDirectory(ResourceManager pResourceManager, String pName, Gson pGson) {
        return scanDirectory(pResourceManager, FileToIdConverter.json(pName), pGson);
    }

    public static Map<ResourceLocation, JsonElement> scanDirectory(ResourceManager pResourceManager, FileToIdConverter filetoidconverter, Gson pGson) {
        Map<ResourceLocation, JsonElement> output = Maps.newHashMap();
        for(Map.Entry<ResourceLocation, Resource> entry : filetoidconverter.listMatchingResources(pResourceManager).entrySet()) {
            ResourceLocation resourcelocation = entry.getKey();

            try (Resource resource = entry.getValue();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
                JsonElement jsonelement = GsonHelper.fromJson(pGson, reader, JsonElement.class, true);
                JsonElement jsonelement1 = output.put(resourcelocation, jsonelement);
                if (jsonelement1 != null) {
                    throw new IllegalStateException("Duplicate data file ignored with ID " + resourcelocation);
                }
            } catch (IllegalArgumentException | IOException | JsonParseException jsonparseexception) {
                GunMod.LOGGER.error("Couldn't parse data file {}", resourcelocation, jsonparseexception);
            }
        }
        return output;
    }

    /**
     * 扫描指定目录下的所有json文件<br/>
     * 与{@link #scanDirectory(ResourceManager, String, Gson)}不同的是，该方法会读取所有json文件作为列表返回
     * @param pResourceManager 资源管理器
     * @param filetoidconverter 文件路径和id的映射
     * @param pGson Gson实例
     * @return 扫描到的json文件
     */
    public static Map<ResourceLocation, List<JsonElement>> scanDirectoryAll(ResourceManager pResourceManager, FileToIdConverter filetoidconverter, Gson pGson) {
        Map<ResourceLocation, List<JsonElement>> output = Maps.newHashMap();
        for(Map.Entry<ResourceLocation, List<Resource>> entry : filetoidconverter.listMatchingResourceStacks(pResourceManager).entrySet()) {
            ResourceLocation resourcelocation = entry.getKey();

            for (Resource resource : entry.getValue()) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
                    JsonElement jsonelement = GsonHelper.fromJson(pGson, reader, JsonElement.class, true);
                    List<JsonElement> list = output.computeIfAbsent(resourcelocation, k -> Lists.newArrayList());
                    list.add(jsonelement);
                } catch (IllegalArgumentException | IOException | JsonParseException jsonparseexception) {
                    GunMod.LOGGER.error("Couldn't parse data file {}", resourcelocation, jsonparseexception);
                }
            }
        }
        return output;
    }
}
