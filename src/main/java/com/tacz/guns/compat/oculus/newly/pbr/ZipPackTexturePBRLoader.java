package com.tacz.guns.compat.oculus.newly.pbr;

import com.tacz.guns.client.resource_legacy.texture.ZipPackTexture;
import net.irisshaders.iris.texture.pbr.PBRType;
import net.irisshaders.iris.texture.pbr.loader.PBRTextureLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

public class ZipPackTexturePBRLoader implements PBRTextureLoader<ZipPackTexture> {
    @Override
    public void load(ZipPackTexture zipPackTexture, ResourceManager resourceManager, PBRTextureConsumer pbrTextureConsumer) {
        ResourceLocation id = zipPackTexture.getRegisterId();
        ResourceLocation pbrNormalId = new ResourceLocation(id.getNamespace(), id.getPath() + PBRType.NORMAL.getSuffix());
        ResourceLocation pbrSpecularId = new ResourceLocation(id.getNamespace(), id.getPath() + PBRType.SPECULAR.getSuffix());
        TextureManager textureManager = Minecraft.getInstance().getTextureManager();
        if (textureManager.byPath.containsKey(pbrNormalId)) {
            pbrTextureConsumer.acceptNormalTexture(textureManager.getTexture(pbrNormalId));
        }
        if (textureManager.byPath.containsKey(pbrSpecularId)) {
            pbrTextureConsumer.acceptSpecularTexture(textureManager.getTexture(pbrSpecularId));
        }
    }
}
