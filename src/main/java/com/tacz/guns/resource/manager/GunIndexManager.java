package com.tacz.guns.resource.manager;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tacz.guns.resource.CommonAssetsManager;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.network.DataType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

public class GunIndexManager extends CommonDataManager<CommonGunIndex> {
    public GunIndexManager() {
        super(DataType.GUN_INDEX, CommonGunIndex.class, CommonAssetsManager.GSON, "index/guns", "GunIndexLoader");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager, ProfilerFiller profiler) {
        super.apply(objects, resourceManager, profiler);

        ImmutableMap.Builder<ResourceLocation, String> builder = ImmutableMap.builder();
        objects.forEach((id, element) -> {
            CommonGunIndex index = getData(id);
            if (index == null) {
                builder.put(id, element.toString());
                return;
            }
            // 因为不会下发脚本，手动同步能否卸载
            JsonObject synced = element.getAsJsonObject().deepCopy();
            synced.addProperty("has_unload_ammo_script", index.hasUnloadAmmoScript());
            builder.put(id, synced.toString());
        });
        networkCache = builder.build();
    }
}
