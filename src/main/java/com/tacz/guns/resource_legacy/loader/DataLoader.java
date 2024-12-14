package com.tacz.guns.resource_legacy.loader;

import com.tacz.guns.api.resource.JsonResourceLoader;
import com.tacz.guns.resource_legacy.CommonAssetManager;
import com.tacz.guns.resource_legacy.CommonGunPackLoader;
import com.tacz.guns.resource_legacy.network.CommonGunPackNetwork;
import com.tacz.guns.resource.network.DataType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.BiConsumer;
@Deprecated
public class DataLoader<T> extends JsonResourceLoader<T> {
    private final DataType dataType;
    protected final BiConsumer<ResourceLocation, T> save;

    public DataLoader(DataType dataType, Class<T> dataClass, String marker, String domain,
                      BiConsumer<ResourceLocation, T> save) {
        super(dataClass, marker, domain);
        this.dataType = dataType;
        this.save = save;
    }

    public DataLoader(DataType dataType, Class<T> dataClass, String marker, String domain) {
        this(dataType, dataClass, marker, domain, CommonAssetManager.INSTANCE::put);
    }

    public DataType getDataType() {
        return dataType;
    }

    @Override
    public void resolveJson(ResourceLocation id, String json) {
        T data = CommonGunPackLoader.GSON.fromJson(json, getDataClass());
        save.accept(id, data);
        CommonGunPackNetwork.addData(getDataType(), id, json);
    }
}
