package com.tacz.guns.client.resource_legacy.loader.asset;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.tacz.guns.GunMod;
import com.tacz.guns.client.resource_legacy.ClientAssetManager;
import com.tacz.guns.client.resource.pojo.model.BedrockModelPOJO;
import com.tacz.guns.util.TacPathVisitor;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.tacz.guns.client.resource_legacy.ClientGunPackLoader.GSON;

public final class BedrockModelLoader {
    private static final Marker MARKER = MarkerManager.getMarker("BedrockModelLoader");
    private static final Pattern MODEL_PATTERN = Pattern.compile("^(\\w+)/models/([\\w/]+)\\.json$");

    public static boolean load(ZipFile zipFile, String zipPath) {
        Matcher matcher = MODEL_PATTERN.matcher(zipPath);
        if (matcher.find()) {
            String namespace = matcher.group(1);
            String path = matcher.group(2);
            ZipEntry entry = zipFile.getEntry(zipPath);
            if (entry == null) {
                GunMod.LOGGER.warn(MARKER, "{} file don't exist", zipPath);
                return false;
            }
            try (InputStream modelFileStream = zipFile.getInputStream(entry)) {
                ResourceLocation registryName = new ResourceLocation(namespace, path);
                BedrockModelPOJO modelPOJO = GSON.fromJson(new InputStreamReader(modelFileStream, StandardCharsets.UTF_8), BedrockModelPOJO.class);
                ClientAssetManager.INSTANCE.putModel(registryName, modelPOJO);
                return true;
            } catch (IOException | JsonSyntaxException | JsonIOException exception) {
                GunMod.LOGGER.warn(MARKER, "Failed to read model file: {}, entry: {}", zipFile, entry);
                exception.printStackTrace();
            }
        }
        return false;
    }

    public static void load(File root) {
        Path modelPath = root.toPath().resolve("models");
        if (Files.isDirectory(modelPath)) {
            TacPathVisitor visitor = new TacPathVisitor(modelPath.toFile(), root.getName(), ".json", (id, file) -> {
                try (InputStream modelFileStream = Files.newInputStream(file)) {
                    BedrockModelPOJO modelPOJO = GSON.fromJson(new InputStreamReader(modelFileStream, StandardCharsets.UTF_8), BedrockModelPOJO.class);
                    ClientAssetManager.INSTANCE.putModel(id, modelPOJO);
                } catch (IOException | JsonSyntaxException | JsonIOException exception) {
                    GunMod.LOGGER.warn(MARKER, "Failed to read model file: {}", file);
                    exception.printStackTrace();
                }
            });
            try {
                Files.walkFileTree(modelPath, visitor);
            } catch (IOException e) {
                GunMod.LOGGER.warn(MARKER, "Failed to walk file tree: {}", modelPath);
                e.printStackTrace();
            }
        }
    }
}
