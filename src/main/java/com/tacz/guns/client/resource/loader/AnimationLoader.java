package com.tacz.guns.client.resource.loader;

import com.tacz.guns.GunMod;
import com.tacz.guns.client.animation.gltf.AnimationStructure;
import com.tacz.guns.client.resource.ClientAssetManager;
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

import static com.tacz.guns.client.resource.ClientGunPackLoader.GSON;

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
            } catch (IOException ioe) {
                // 可能用来判定错误，打印下
                GunMod.LOGGER.warn(MARKER, "Failed to load animation: {}", zipPath);
                ioe.printStackTrace();
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
            } catch (IOException ioe) {
                // 可能用来判定错误，打印下
                GunMod.LOGGER.warn(MARKER, "Failed to load animation: {}", zipPath);
                ioe.printStackTrace();
            }
        }
        return false;
    }

    public static void load(File root) throws IOException {
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
            Files.walkFileTree(animationPath, gltfVisitor);
            TacPathVisitor bedrockVisitor = new TacPathVisitor(animationPath.toFile(), root.getName(), ".animation.json", (id, file) -> {
                try (InputStream animationFileStream = Files.newInputStream(file)) {
                    BedrockAnimationFile bedrockAnimationFile = GSON.fromJson(new InputStreamReader(animationFileStream, StandardCharsets.UTF_8), BedrockAnimationFile.class);
                    ClientAssetManager.INSTANCE.putBedrockAnimation(id, bedrockAnimationFile);
                } catch (IOException exception) {
                    GunMod.LOGGER.warn(MARKER, "Failed to read animation file: {}", file);
                    exception.printStackTrace();
                }
            });
            Files.walkFileTree(animationPath, bedrockVisitor);
        }
    }
}
