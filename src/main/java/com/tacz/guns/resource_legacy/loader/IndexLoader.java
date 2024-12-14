package com.tacz.guns.resource_legacy.loader;

import com.tacz.guns.resource_legacy.CommonGunPackLoader;
import com.tacz.guns.resource.network.DataType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class IndexLoader<R, T> extends DataLoader<T> {
    private final Function<R, T> converter;
    private final Class<R> raw;

    public IndexLoader(DataType dataType, Class<R> raw, Class<T> dataClass,
                       String marker, String domain, BiConsumer<ResourceLocation, T> save, Function<R, T> converter) {
        super(dataType, dataClass, marker, domain, save);
        this.converter = converter;
        this.raw = raw;
    }

    public Class<R> getRaw() {
        return raw;
    }

    @Override
    public void resolveJson(ResourceLocation id, String json) {
        R raw = CommonGunPackLoader.GSON.fromJson(json, getRaw());
        T data = converter.apply(raw);
        save.accept(id, data);
    }
}
