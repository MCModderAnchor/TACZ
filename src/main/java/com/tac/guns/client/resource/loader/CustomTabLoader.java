package com.tac.guns.client.resource.loader;

import com.google.common.reflect.TypeToken;
import com.tac.guns.GunMod;
import com.tac.guns.client.resource.ClientAssetManager;
import com.tac.guns.client.resource.pojo.CustomTabPOJO;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.tac.guns.client.resource.ClientGunPackLoader.GSON;

public class CustomTabLoader {
    private static final Marker MARKER = MarkerManager.getMarker("CreativeTabLoader");
    private static final Pattern TAB_PATTERN = Pattern.compile("^\\w+/tab\\.json$");

    @SuppressWarnings("UnstableApiUsage")
    public static boolean load(ZipFile zipFile, String zipPath) {
        Matcher matcher = TAB_PATTERN.matcher(zipPath);
        if (matcher.matches()) {
            ZipEntry entry = zipFile.getEntry(zipPath);
            if (entry == null) {
                GunMod.LOGGER.warn(MARKER, "{} file don't exist", zipPath);
                return false;
            }
            try (InputStream inputStream = zipFile.getInputStream(entry)) {
                Type type = new TypeToken<Map<String, CustomTabPOJO>>() {
                }.getType();
                Map<String, CustomTabPOJO> customTabs = GSON.fromJson(new InputStreamReader(inputStream, StandardCharsets.UTF_8), type);
                ClientAssetManager.INSTANCE.putAllCustomTab(customTabs);
                return true;
            } catch (IOException ioe) {
                // 可能用来判定错误，打印下
                GunMod.LOGGER.warn(MARKER, "Failed to load tab json: {}", zipPath);
                ioe.printStackTrace();
            }
        }
        return false;
    }

    @SuppressWarnings("UnstableApiUsage")
    public static void load(File root) throws IOException {
        Path tabFilePath = root.toPath().resolve("tab.json");
        if (Files.isRegularFile(tabFilePath)) {
            try (InputStream stream = Files.newInputStream(tabFilePath)) {
                Type type = new TypeToken<Map<String, CustomTabPOJO>>() {
                }.getType();
                Map<String, CustomTabPOJO> customTabs = GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), type);
                ClientAssetManager.INSTANCE.putAllCustomTab(customTabs);
            } catch (IOException exception) {
                GunMod.LOGGER.warn(MARKER, "Failed to read tab json: {}", tabFilePath);
                exception.printStackTrace();
            }
        }
    }
}
