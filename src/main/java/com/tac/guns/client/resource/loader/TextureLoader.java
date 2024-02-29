package com.tac.guns.client.resource.loader;

import com.tac.guns.GunMod;
import com.tac.guns.client.resource.texture.ZipPackTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class TextureLoader {
    private static final Marker MARKER = MarkerManager.getMarker("TextureLoader");
    private static final Pattern TEXTURE_PATTERN = Pattern.compile("^(\\w+)/textures/([\\w/]+)\\.png$");

    public static boolean load(ZipFile zipFile, String zipPath) {
        Matcher matcher = TEXTURE_PATTERN.matcher(zipPath);
        if (matcher.find()) {
            String namespace = matcher.group(1);
            String path = matcher.group(2);
            String filePath = String.format("%s/textures/%s.png", namespace, path);
            ZipEntry entry = zipFile.getEntry(filePath);
            if (entry == null) {
                GunMod.LOGGER.warn(MARKER, "{} file don't exist", filePath);
                return false;
            }
            ResourceLocation registryName = new ResourceLocation(namespace, path);
            ZipPackTexture zipPackTexture = new ZipPackTexture(zipFile.getName(), namespace, path);
            Minecraft.getInstance().textureManager.register(registryName, zipPackTexture);
            return true;
        }
        return false;
    }
}
