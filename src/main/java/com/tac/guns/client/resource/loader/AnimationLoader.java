package com.tac.guns.client.resource.loader;

import com.tac.guns.GunMod;
import com.tac.guns.client.animation.gltf.AnimationStructure;
import com.tac.guns.client.resource.ClientAssetManager;
import com.tac.guns.client.resource.pojo.animation.gltf.RawAnimationStructure;
import com.tac.guns.util.TacPathVisitor;
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

import static com.tac.guns.client.resource.ClientGunPackLoader.GSON;

public final class AnimationLoader {
    private static final Marker MARKER = MarkerManager.getMarker("AnimationLoader");
    private static final Pattern ANIMATION_PATTERN = Pattern.compile("^(\\w+)/animations/([\\w/]+)\\.gltf$");

    public static boolean load(ZipFile zipFile, String zipPath) {
        Matcher matcher = ANIMATION_PATTERN.matcher(zipPath);
        if (matcher.find()) {
            String namespace = matcher.group(1);
            String path = matcher.group(2);
            ZipEntry entry = zipFile.getEntry(zipPath);
            if (entry == null) {
                GunMod.LOGGER.warn(MARKER, "{} file don't exist", zipPath);
                return false;
            }
            try (InputStream animationFileStream = zipFile.getInputStream(entry)) {
                ResourceLocation registryName = new ResourceLocation(namespace, path);
                RawAnimationStructure rawStructure = GSON.fromJson(new InputStreamReader(animationFileStream, StandardCharsets.UTF_8), RawAnimationStructure.class);
                ClientAssetManager.INSTANCE.putAnimation(registryName, new AnimationStructure(rawStructure));
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
            TacPathVisitor visitor = new TacPathVisitor(animationPath.toFile(), root.getName(), ".gltf", (id, file) -> {
                try (InputStream animationFileStream = Files.newInputStream(file)) {
                    RawAnimationStructure rawStructure = GSON.fromJson(new InputStreamReader(animationFileStream, StandardCharsets.UTF_8), RawAnimationStructure.class);
                    ClientAssetManager.INSTANCE.putAnimation(id, new AnimationStructure(rawStructure));
                } catch (IOException exception) {
                    GunMod.LOGGER.warn(MARKER, "Failed to read animation file: {}", file);
                    exception.printStackTrace();
                }
            });
            Files.walkFileTree(animationPath, visitor);
        }
    }
}
