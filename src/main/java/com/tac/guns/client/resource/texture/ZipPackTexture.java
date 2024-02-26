package com.tac.guns.client.resource.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipPackTexture extends AbstractTexture {
    private final String texturePath;
    private final Path zipFilePath;

    public ZipPackTexture(String zipFilePath, String texturePath) {
        this.zipFilePath = Paths.get(zipFilePath);
        this.texturePath = texturePath;
    }

    public boolean isExist() {
        try (ZipFile zipFile = new ZipFile(zipFilePath.toFile())) {
            ZipEntry entry = zipFile.getEntry(String.format("textures/%s", texturePath));
            return entry != null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
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
        try (ZipFile zipFile = new ZipFile(zipFilePath.toFile())) {
            ZipEntry entry = zipFile.getEntry(String.format("textures/%s", texturePath));
            if (entry == null) {
                return;
            }
            try (InputStream stream = zipFile.getInputStream(entry)) {
                NativeImage imageIn = NativeImage.read(stream);
                int width = imageIn.getWidth();
                int height = imageIn.getHeight();
                TextureUtil.prepareImage(this.getId(), 0, width, height);
                imageIn.upload(0, 0, 0, 0, 0, width, height, false, false, false, true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
