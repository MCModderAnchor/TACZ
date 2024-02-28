package com.tac.guns.client.resource.loader;

import com.google.common.collect.Sets;
import com.tac.guns.client.resource.texture.ZipPackTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;

public final class TextureLoader {
    /**
     * 如果从zip包中加载、注册材质，自动在命名空间后加上此后缀，以作区分。
     */
    public static final String ZIP_TEXTURE_NAMESPACE_SUFFIX = "-zip";
    /**
     * 缓存已经注册的材质的注册名，避免重复注册浪费内存。
     */
    public static final Set<ResourceLocation> TMP_REGISTER_TEXTURE = Sets.newHashSet();

    /**
     * @return 材质的实际注册名，方便调用
     */
    public static ResourceLocation loadTexture(String zipFilePath, ResourceLocation texturePath) {
        ResourceLocation registryName = new ResourceLocation(texturePath.getNamespace() + ZIP_TEXTURE_NAMESPACE_SUFFIX, texturePath.getPath());
        if (!TMP_REGISTER_TEXTURE.contains(registryName)) {
            ZipPackTexture zipPackTexture = new ZipPackTexture(zipFilePath, texturePath.getNamespace(), texturePath.getPath());
            if (zipPackTexture.isExist()) {
                Minecraft.getInstance().textureManager.register(registryName, zipPackTexture);
                TMP_REGISTER_TEXTURE.add(registryName);
            }
        }
        return registryName;
    }
}
