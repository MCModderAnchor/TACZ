package com.tacz.guns.client.resource_legacy.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;


public class FilePackTexture extends AbstractTexture {
    private final ResourceLocation registerId;
    private final Path filePath;

    public FilePackTexture(ResourceLocation registerId, Path filePath) {
        this.registerId = registerId;
        this.filePath = filePath;
    }

    @Override
    public void load(ResourceManager manager) {
        if (!RenderSystem.isOnRenderThreadOrInit()) {
            RenderSystem.recordRenderCall(this::doLoad);
        } else {
            this.doLoad();
        }
    }

    private void doLoad() {
        File textureFile = filePath.toFile();
        if (textureFile.isFile()) {
            try (InputStream stream = Files.newInputStream(textureFile.toPath())) {
                NativeImage imageIn = NativeImage.read(stream);
                int width = imageIn.getWidth();
                int height = imageIn.getHeight();
                TextureUtil.prepareImage(this.getId(), 0, width, height);
                imageIn.upload(0, 0, 0, 0, 0, width, height, false, false, false, true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public ResourceLocation getRegisterId() {
        return registerId;
    }
}
