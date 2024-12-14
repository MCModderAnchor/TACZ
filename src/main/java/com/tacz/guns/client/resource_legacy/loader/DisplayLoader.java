package com.tacz.guns.client.resource_legacy.loader;

import com.tacz.guns.api.resource.JsonResourceLoader;
import com.tacz.guns.client.resource_legacy.ClientGunPackLoader;
import net.minecraft.resources.ResourceLocation;

import java.io.File;
import java.util.function.BiConsumer;

public class DisplayLoader<T> extends JsonResourceLoader<T> {
    protected final BiConsumer<ResourceLocation, T> save;

    public DisplayLoader(Class<T> dataClass, String marker, String domain,
                         BiConsumer<ResourceLocation, T> save) {
        super(dataClass, marker, domain);
        this.save = save;
    }

    @Override
    public void load(File root) {
        super.load(root);
    }

    @Override
    public void resolveJson(ResourceLocation id, String json) {
        T data = ClientGunPackLoader.GSON.fromJson(json, getDataClass());
        save.accept(id, data);
    }
}
