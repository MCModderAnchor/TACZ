package com.tacz.guns.client.resource.manager;

import com.google.common.collect.Maps;
import com.google.gson.JsonParseException;
import com.tacz.guns.GunMod;
import com.tacz.guns.client.resource.pojo.PackInfo;
import com.tacz.guns.resource.CommonAssetsManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;

public class PackInfoManager extends SimplePreparableReloadListener<Map<String, PackInfo>> {
    private static final Marker MARKER = MarkerManager.getMarker("PackInfoLoader");
    private static final String PACK_INFO_NAME = "gunpack_info.json";
    private final Map<String, PackInfo> dataMap = Maps.newHashMap();

    @Override
    protected Map<String, PackInfo> prepare(ResourceManager manager, ProfilerFiller pProfiler) {
        Map<String, PackInfo> output = Maps.newHashMap();

        for (String namespaces : manager.getNamespaces()) {
            manager.getResource(new ResourceLocation(namespaces, PACK_INFO_NAME)).ifPresent(rl -> {
                try (Reader reader = rl.openAsReader()) {
                    PackInfo packInfo = GsonHelper.fromJson(CommonAssetsManager.GSON, reader, PackInfo.class, true);
                    PackInfo packInfo1 = output.put(namespaces, packInfo);
                    if (packInfo1 != null) {
                        throw new IllegalStateException("Duplicate data file ignored with namespace " + namespaces);
                    }
                } catch (IllegalArgumentException | IOException | JsonParseException jsonparseexception) {
                    GunMod.LOGGER.error(MARKER, "Couldn't parse pack info for namespace '{}' from {}", namespaces, rl, jsonparseexception);
                }
            });
        }
        return output;
    }

    @Override
    protected void apply(Map<String, PackInfo> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        dataMap.clear();
        dataMap.putAll(pObject);
    }

    public PackInfo getData(String namespace) {
        return dataMap.get(namespace);
    }
}
