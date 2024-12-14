package com.tacz.guns.client.resource_legacy.loader.asset;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.client.animation.gltf.AnimationStructure;
import com.tacz.guns.client.resource_legacy.ClientAssetManager;
import com.tacz.guns.client.resource.pojo.animation.bedrock.BedrockAnimationFile;
import com.tacz.guns.client.resource.pojo.animation.gltf.RawAnimationStructure;
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

public final class AnimationLoader {
    private static final Marker MARKER = MarkerManager.getMarker("AnimationLoader");
    private static final Pattern GLTF_ANIMATION_PATTERN = Pattern.compile("^(\\w+)/animations/([\\w/]+)\\.gltf$");
    private static final Pattern BEDROCK_ANIMATION_PATTERN = Pattern.compile("^(\\w+)/animations/([\\w/]+)\\.animation\\.json$");

    public static boolean load(ZipFile zipFile, String zipPath) {
        Matcher gltfMatcher = GLTF_ANIMATION_PATTERN.matcher(zipPath);
        if (gltfMatcher.find()) {
            String namespace = gltfMatcher.group(1);
            String path = gltfMatcher.group(2);
            ZipEntry entry = zipFile.getEntry(zipPath);
            if (entry == null) {
                GunMod.LOGGER.warn(MARKER, "{} file don't exist", zipPath);
                return false;
            }
            try (InputStream animationFileStream = zipFile.getInputStream(entry)) {
                ResourceLocation registryName = new ResourceLocation(namespace, path);
                RawAnimationStructure rawStructure = GSON.fromJson(new InputStreamReader(animationFileStream, StandardCharsets.UTF_8), RawAnimationStructure.class);
                ClientAssetManager.INSTANCE.putGltfAnimation(registryName, new AnimationStructure(rawStructure));
            } catch (IOException | JsonSyntaxException | JsonIOException exception) {
                GunMod.LOGGER.warn(MARKER, "Failed to read animation file: {}, entry: {}", zipFile, entry);
                exception.printStackTrace();
            }
        }
        Matcher bedrockMatcher = BEDROCK_ANIMATION_PATTERN.matcher(zipPath);
        if (bedrockMatcher.find()) {
            String namespace = bedrockMatcher.group(1);
            String path = bedrockMatcher.group(2);
            ZipEntry entry = zipFile.getEntry(zipPath);
            if (entry == null) {
                GunMod.LOGGER.warn(MARKER, "{} file don't exist", zipPath);
                return false;
            }
            try (InputStream animationFileStream = zipFile.getInputStream(entry)) {
                ResourceLocation registryName = new ResourceLocation(namespace, path);
                BedrockAnimationFile bedrockAnimationFile = GSON.fromJson(new InputStreamReader(animationFileStream, StandardCharsets.UTF_8), BedrockAnimationFile.class);
                ClientAssetManager.INSTANCE.putBedrockAnimation(registryName, bedrockAnimationFile);
            } catch (IOException | JsonSyntaxException | JsonIOException exception) {
                GunMod.LOGGER.warn(MARKER, "Failed to read animation file: {}, entry: {}", zipFile, entry);
                exception.printStackTrace();
            }
        }
        return false;
    }

    public static void load(File root) {
        Path animationPath = root.toPath().resolve("animations");
        if (Files.isDirectory(animationPath)) {
            TacPathVisitor gltfVisitor = new TacPathVisitor(animationPath.toFile(), root.getName(), ".gltf", (id, file) -> {
                try (InputStream animationFileStream = Files.newInputStream(file)) {
                    RawAnimationStructure rawStructure = GSON.fromJson(new InputStreamReader(animationFileStream, StandardCharsets.UTF_8), RawAnimationStructure.class);
                    ClientAssetManager.INSTANCE.putGltfAnimation(id, new AnimationStructure(rawStructure));
                } catch (IOException exception) {
                    GunMod.LOGGER.warn(MARKER, "Failed to read animation file: {}", file);
                    exception.printStackTrace();
                }
            });
            try {
                Files.walkFileTree(animationPath, gltfVisitor);
            } catch (IOException | JsonSyntaxException | JsonIOException exception) {
                GunMod.LOGGER.warn(MARKER, "Failed to walk file tree: {}", animationPath);
                exception.printStackTrace();
            }

            TacPathVisitor bedrockVisitor = new TacPathVisitor(animationPath.toFile(), root.getName(), ".animation.json", (id, file) -> {
                try (InputStream animationFileStream = Files.newInputStream(file)) {
                    BedrockAnimationFile bedrockAnimationFile = GSON.fromJson(new InputStreamReader(animationFileStream, StandardCharsets.UTF_8), BedrockAnimationFile.class);
                    ClientAssetManager.INSTANCE.putBedrockAnimation(id, bedrockAnimationFile);
                } catch (IOException | JsonSyntaxException | JsonIOException exception) {
                    GunMod.LOGGER.warn(MARKER, "Failed to read animation file: {}", file);
                    exception.printStackTrace();
                }
            });
            try {
                Files.walkFileTree(animationPath, bedrockVisitor);
            } catch (IOException e) {
                GunMod.LOGGER.warn(MARKER, "Failed to walk file tree: {}", animationPath);
                e.printStackTrace();
            }
        }
    }
}
