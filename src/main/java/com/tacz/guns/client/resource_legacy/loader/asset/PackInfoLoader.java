package com.tacz.guns.client.resource_legacy.loader.asset;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.tacz.guns.GunMod;
import com.tacz.guns.client.resource_legacy.ClientAssetManager;
import com.tacz.guns.client.resource.pojo.PackInfo;
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

public final class PackInfoLoader {
    private static final Marker MARKER = MarkerManager.getMarker("CreativeTabLoader");
    private static final Pattern PACK_INFO_PATTERN = Pattern.compile("^(\\w+)/pack\\.json$");

    @SuppressWarnings("UnstableApiUsage")
    public static boolean load(ZipFile zipFile, String zipPath) {
        Matcher matcher = PACK_INFO_PATTERN.matcher(zipPath);
        if (matcher.find()) {
            String namespace = matcher.group(1);
            ZipEntry entry = zipFile.getEntry(zipPath);
            if (entry == null) {
                GunMod.LOGGER.warn(MARKER, "{} file don't exist", zipPath);
                return false;
            }
            try (InputStream inputStream = zipFile.getInputStream(entry)) {
                PackInfo packInfo = GSON.fromJson(new InputStreamReader(inputStream, StandardCharsets.UTF_8), PackInfo.class);
                ClientAssetManager.INSTANCE.putPackInfo(namespace, packInfo);
                return true;
            } catch (IOException | JsonSyntaxException | JsonIOException exception) {
                GunMod.LOGGER.warn(MARKER, "Failed to read info json: {}, entry: {}", zipFile, entry);
                exception.printStackTrace();
            }
        }
        return false;
    }

    @SuppressWarnings("UnstableApiUsage")
    public static void load(File root) {
        Path packInfoFilePath = root.toPath().resolve("pack.json");
        if (Files.isRegularFile(packInfoFilePath)) {
            try (InputStream stream = Files.newInputStream(packInfoFilePath)) {
                PackInfo packInfo = GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), PackInfo.class);
                ClientAssetManager.INSTANCE.putPackInfo(root.getName(), packInfo);
            } catch (IOException | JsonSyntaxException | JsonIOException exception) {
                GunMod.LOGGER.warn(MARKER, "Failed to read info json: {}", packInfoFilePath);
                exception.printStackTrace();
            }
        }
    }
}
