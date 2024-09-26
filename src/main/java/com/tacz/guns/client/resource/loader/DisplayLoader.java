package com.tacz.guns.client.resource.loader;

import com.tacz.guns.api.resource.JsonResourceLoader;
import com.tacz.guns.client.resource.ClientGunPackLoader;
import com.tacz.guns.resource.CommonAssetManager;
import com.tacz.guns.resource.CommonGunPackLoader;
import com.tacz.guns.resource.network.CommonGunPackNetwork;
import com.tacz.guns.resource.network.DataType;
import net.minecraft.resources.ResourceLocation;

import java.io.File;
import java.util.function.BiConsumer;
import java.util.zip.ZipFile;

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
