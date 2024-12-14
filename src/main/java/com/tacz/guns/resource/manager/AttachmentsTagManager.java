package com.tacz.guns.resource.manager;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.tacz.guns.GunMod;
import com.tacz.guns.resource.CommonAssetsManager;
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

import java.util.*;


public class AttachmentsTagManager extends SimplePreparableReloadListener<Map<ResourceLocation, List<JsonElement>>> implements INetworkCacheReloadListener {
    private final Map<ResourceLocation, Set<String>> tags = Maps.newHashMap();
    private final Map<ResourceLocation, Set<String>> allow_attachments = Maps.newHashMap();

    private final Gson gson;
    private final Marker marker;

    private final FileToIdConverter fileToIdConverter;
    protected Map<ResourceLocation, String> networkCache;

    public AttachmentsTagManager() {
        this.gson = CommonAssetsManager.GSON;
        this.marker = MarkerManager.getMarker("AllowTagManager");
        this.fileToIdConverter = FileToIdConverter.json("tacz_tags/attachments");
    }

    @NotNull
    @Override
    protected Map<ResourceLocation, List<JsonElement>> prepare(ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        return ResourceScanner.scanDirectoryAll(pResourceManager, this.fileToIdConverter, this.gson);
    }

    @Override
    protected void apply(Map<ResourceLocation, List<JsonElement>> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        tags.clear();
        allow_attachments.clear();
        ImmutableMap.Builder<ResourceLocation, String> builder = ImmutableMap.builder();

        for (Map.Entry<ResourceLocation, List<JsonElement>> entry : pObject.entrySet()) {
            ResourceLocation id = entry.getKey();

            List<String> temp = new ArrayList<>();
            for (JsonElement element : entry.getValue()) {
                try {
                    List<String> data = parseJson(element);
                    if (data != null) {
                        temp.addAll(data);
                    }
                } catch (JsonParseException e) {
                    GunMod.LOGGER.error(marker, "Failed to load data file {}", id, e);
                }
            }

            if (id.getPath().startsWith("allow_attachments/") && id.getPath().length()>18) {
                ResourceLocation gunId = id.withPath(id.getPath().substring(18));
                allow_attachments.computeIfAbsent(gunId, (v) -> Sets.newHashSet()).addAll(temp);
            } else {
                tags.computeIfAbsent(id, (v) -> Sets.newHashSet()).addAll(temp);
            }

            builder.put(entry.getKey(), gson.toJson(temp));
        }

        this.networkCache = builder.build();
    }

    private List<String> parseJson(JsonElement element) {
        return gson.fromJson(element, new TypeToken<>(){});
    }

    @Override
    public Map<ResourceLocation, String> getNetworkCache() {
        return networkCache;
    }

    @Override
    public DataType getType() {
        return DataType.ATTACHMENT_TAGS;
    }

    public Set<String> getAttachmentTags(ResourceLocation registryName) {
        return tags.get(registryName);
    }

    public Set<String> getAllowAttachmentTags(ResourceLocation registryName) {
        return allow_attachments.get(registryName);
    }
}
