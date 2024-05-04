package com.tacz.guns.client.resource.loader;

import com.tacz.guns.GunMod;
import com.tacz.guns.client.resource.ClientAssetManager;
import com.tacz.guns.client.resource.ClientGunPackLoader;
import com.tacz.guns.client.resource.pojo.display.ammo.AmmoDisplay;
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

public class AmmoDisplayLoader {
    private static final Marker MARKER = MarkerManager.getMarker("AmmoDisplayLoader");
    private static final Pattern DISPLAY_PATTERN = Pattern.compile("^(\\w+)/ammo/display/([\\w/]+)\\.json$");

    public static boolean load(ZipFile zipFile, String zipPath) throws IOException {
        Matcher matcher = DISPLAY_PATTERN.matcher(zipPath);
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
                AmmoDisplay display = ClientGunPackLoader.GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), AmmoDisplay.class);
                ClientAssetManager.INSTANCE.putAmmoDisplay(registryName, display);
                return true;
            }
        }
        return false;
    }

    public static void load(File root) throws IOException {
        Path displayPath = root.toPath().resolve("ammo/display");
        if (Files.isDirectory(displayPath)) {
            TacPathVisitor visitor = new TacPathVisitor(displayPath.toFile(), root.getName(), ".json", (id, file) -> {
                try (InputStream stream = Files.newInputStream(file)) {
                    AmmoDisplay display = ClientGunPackLoader.GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), AmmoDisplay.class);
                    ClientAssetManager.INSTANCE.putAmmoDisplay(id, display);
                } catch (IOException exception) {
                    GunMod.LOGGER.warn(MARKER, "Failed to read display file: {}", file);
                    exception.printStackTrace();
                }
            });
            Files.walkFileTree(displayPath, visitor);
        }
    }
}
