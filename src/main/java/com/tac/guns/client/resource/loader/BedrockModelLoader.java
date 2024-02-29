package com.tac.guns.client.resource.loader;

import com.tac.guns.GunMod;
import com.tac.guns.client.resource.cache.ClientAssetManager;
import com.tac.guns.client.resource.pojo.model.BedrockModelPOJO;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.tac.guns.client.resource.ClientGunLoader.GSON;

public final class BedrockModelLoader {
    private static final Marker MARKER = MarkerManager.getMarker("BedrockModelLoader");
    private static final Pattern MODEL_PATTERN = Pattern.compile("^(\\w+)/models/([\\w/]+)\\.json$");

    public static boolean load(ZipFile zipFile, String zipPath) {
        Matcher matcher = MODEL_PATTERN.matcher(zipPath);
        if (matcher.find()) {
            String namespace = matcher.group(1);
            String path = matcher.group(2);
            String filePath = String.format("%s/models/%s.json", namespace, path);
            ZipEntry entry = zipFile.getEntry(filePath);
            if (entry == null) {
                GunMod.LOGGER.warn(MARKER, "{} file don't exist", filePath);
                return false;
            }
            try (InputStream modelFileStream = zipFile.getInputStream(entry)) {
                ResourceLocation registryName = new ResourceLocation(namespace, path);
                BedrockModelPOJO modelPOJO = GSON.fromJson(new InputStreamReader(modelFileStream, StandardCharsets.UTF_8), BedrockModelPOJO.class);
                ClientAssetManager.INSTANCE.putModel(registryName, modelPOJO);
                return true;
            } catch (IOException ioe) {
                // 可能用来判定错误，打印下
                GunMod.LOGGER.warn(MARKER, "Failed to load model: {}", filePath);
                ioe.printStackTrace();
            }
        }
        return false;
    }
}
