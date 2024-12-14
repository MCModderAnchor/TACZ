package com.tacz.guns.util;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

public class FileToIdConverter {
    private final String prefix;
    private final String extension;

    public FileToIdConverter(String pPrefix, String pExtenstion) {
        this.prefix = pPrefix;
        this.extension = pExtenstion;
    }

    public static FileToIdConverter json(String pName) {
        return new FileToIdConverter(pName, ".json");
    }

    public ResourceLocation idToFile(ResourceLocation pId) {
        String pPath = this.prefix + "/" + pId.getPath() + this.extension;
        return new ResourceLocation(pId.getNamespace(), pPath);
    }

    public ResourceLocation fileToId(ResourceLocation pFile) {
        String s = pFile.getPath();
        return new ResourceLocation(pFile.getNamespace(), s.substring(this.prefix.length() + 1, s.length() - this.extension.length()));
    }

    public Map<ResourceLocation, Resource> listMatchingResources(ResourceManager pResourceManager) {
        var list = pResourceManager.listResources(this.prefix, s -> s.endsWith(this.extension));
        ImmutableMap.Builder<ResourceLocation, Resource> builder = ImmutableMap.builder();
        list.forEach((p_251984_) -> {
            ResourceLocation resourcelocation = this.fileToId(p_251984_);
            try {
                builder.put(resourcelocation, pResourceManager.getResource(p_251984_));
            } catch (IOException ignore) {}
        });
        return builder.build();
    }

    public Map<ResourceLocation, List<Resource>> listMatchingResourceStacks(ResourceManager pResourceManager) {
        var list = pResourceManager.listResources(this.prefix, s -> s.endsWith(this.extension));
        ImmutableMap.Builder<ResourceLocation, List<Resource>> builder = ImmutableMap.builder();
        list.forEach((p_251984_) -> {
            ResourceLocation resourcelocation = this.fileToId(p_251984_);
            try {
                builder.put(resourcelocation, pResourceManager.getResources(p_251984_));
            } catch (IOException ignore) {}
        });
        return builder.build();
    }
}