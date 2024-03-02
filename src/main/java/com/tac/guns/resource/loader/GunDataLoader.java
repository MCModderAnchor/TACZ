package com.tac.guns.resource.loader;

import com.tac.guns.GunMod;
import com.tac.guns.resource.CommonAssetManager;
import com.tac.guns.resource.CommonGunPackLoader;
import com.tac.guns.resource.pojo.data.GunData;
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

public final class GunDataLoader {
    private static final Marker MARKER = MarkerManager.getMarker("GunDataLoader");
    private static final Pattern DATA_PATTERN = Pattern.compile("^(\\w+)/guns/data/([\\w/]+)\\.json$");

    public static boolean load(ZipFile zipFile, String zipPath) throws IOException {
        Matcher matcher = DATA_PATTERN.matcher(zipPath);
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
                GunData data = CommonGunPackLoader.GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), GunData.class);
                CommonAssetManager.INSTANCE.putGunData(registryName, data);
                return true;
            }
        }
        return false;
    }

    public static void load(File root) throws IOException {
        Path filePath = root.toPath().resolve("guns/data");
        if (Files.isDirectory(filePath)) {
            TacPathVisitor visitor = new TacPathVisitor(filePath.toFile(), root.getName(), ".json", (id, file) -> {
                try (InputStream stream = Files.newInputStream(file)) {
                    GunData data = CommonGunPackLoader.GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), GunData.class);
                    CommonAssetManager.INSTANCE.putGunData(id, data);
                } catch (IOException exception) {
                    GunMod.LOGGER.warn(MARKER, "Failed to read data file: {}", file);
                    exception.printStackTrace();
                }
            });
            Files.walkFileTree(filePath, visitor);
        }
    }
}
