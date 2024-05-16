package com.tacz.guns.compat.oculus.pbr;

import com.tacz.guns.client.resource.texture.FilePackTexture;
import com.tacz.guns.client.resource.texture.ZipPackTexture;
import net.coderbot.iris.texture.pbr.loader.PBRTextureLoaderRegistry;

public class PBRRegister {
    public static void registerPBRLoader() {
        PBRTextureLoaderRegistry.INSTANCE.register(FilePackTexture.class, new FilePackTexturePBRLoader());
        PBRTextureLoaderRegistry.INSTANCE.register(ZipPackTexture.class, new ZipPackTexturePBRLoader());
    }
}
