package com.tacz.guns.util;

import java.util.List;
import java.util.Map;

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
        String pPath = pFile.getPath();
        return new ResourceLocation(pFile.getNamespace(), pPath);
    }

    public Map<ResourceLocation, Resource> listMatchingResources(ResourceManager pResourceManager) {
        return pResourceManager.listResources(this.prefix, (p_251986_) -> {
            return p_251986_.getPath().endsWith(this.extension);
        });
    }

    public Map<ResourceLocation, List<Resource>> listMatchingResourceStacks(ResourceManager pResourceManager) {
        return pResourceManager.listResourceStacks(this.prefix, (p_248700_) -> {
            return p_248700_.getPath().endsWith(this.extension);
        });
    }
}