package com.tacz.guns.compat.playeranimator.animation;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.tacz.guns.GunMod;
import com.tacz.guns.util.TacPathVisitor;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class PlayerAnimatorLoader {
    private static final Marker MARKER = MarkerManager.getMarker("PlayerAnimatorLoader");
    private static final Pattern ANIMATOR_PATTERN = Pattern.compile("^(\\w+)/player_animator/([\\w/]+)\\.json$");

    public static boolean load(ZipFile zipFile, String zipPath) {
        Matcher matcher = ANIMATOR_PATTERN.matcher(zipPath);
        if (matcher.find()) {
            String namespace = matcher.group(1);
            String path = matcher.group(2);
            ZipEntry entry = zipFile.getEntry(zipPath);
            if (entry == null) {
                GunMod.LOGGER.warn(MARKER, "{} file don't exist", zipPath);
                return false;
            }
            try (InputStream stream = zipFile.getInputStream(entry)) {
                ResourceLocation registryName = new ResourceLocation(namespace, path);
                PlayerAnimatorAssetManager.get().putAnimation(registryName, stream);
                return true;
            } catch (IOException | JsonSyntaxException | JsonIOException exception) {
                GunMod.LOGGER.warn(MARKER, "Failed to read player animator file: {}, entry: {}", zipFile, entry);
                exception.printStackTrace();
            }
        }
        return false;
    }

    public static void load(File root) {
        Path playerAnimatorPath = root.toPath().resolve("player_animator");
        if (Files.isDirectory(playerAnimatorPath)) {
            TacPathVisitor visitor = new TacPathVisitor(playerAnimatorPath.toFile(), root.getName(), ".json", (id, file) -> {
                try (InputStream stream = Files.newInputStream(file)) {
                    PlayerAnimatorAssetManager.get().putAnimation(id, stream);
                } catch (IOException | JsonSyntaxException | JsonIOException exception) {
                    GunMod.LOGGER.warn(MARKER, "Failed to read player animator file: {}", file);
                    exception.printStackTrace();
                }
            });
            try {
                Files.walkFileTree(playerAnimatorPath, visitor);
            } catch (IOException e) {
                GunMod.LOGGER.warn(MARKER, "Failed to walk file tree: {}", playerAnimatorPath);
                e.printStackTrace();
            }
        }
    }
}
