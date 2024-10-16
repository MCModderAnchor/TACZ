package com.tacz.guns.resource.loader.asset;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.tacz.guns.GunMod;
import com.tacz.guns.resource.CommonAssetManager;
import com.tacz.guns.resource.CommonGunPackLoader;
import com.tacz.guns.resource.network.CommonGunPackNetwork;
import com.tacz.guns.resource.network.DataType;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.util.TacPathVisitor;
import com.tacz.guns.api.item.gun.FireMode;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.tacz.guns.config.common.GunConfig.ENABLE_GUN_SAFETY;

public final class GunDataLoader {
    private static final Marker MARKER = MarkerManager.getMarker("GunDataLoader");
    private static final Pattern DATA_PATTERN = Pattern.compile("^(\\w+)/guns/data/([\\w/]+)\\.json$");

    public static boolean load(ZipFile zipFile, String zipPath) {
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
                String json = IOUtils.toString(stream, StandardCharsets.UTF_8);
                loadFromJsonString(registryName, json);
                CommonGunPackNetwork.addData(DataType.GUN_DATA, registryName, json);
                return true;
            } catch (IOException | JsonSyntaxException | JsonIOException exception) {
                GunMod.LOGGER.warn(MARKER, "Failed to read data file: {}, entry: {}", zipFile, entry);
                exception.printStackTrace();
            }
        }
        return false;
    }

    public static void load(File root) {
        Path filePath = root.toPath().resolve("guns/data");
        if (Files.isDirectory(filePath)) {
            TacPathVisitor visitor = new TacPathVisitor(filePath.toFile(), root.getName(), ".json", (id, file) -> {
                try (InputStream stream = Files.newInputStream(file)) {
                    String json = IOUtils.toString(stream, StandardCharsets.UTF_8);
                    loadFromJsonString(id, json);
                    CommonGunPackNetwork.addData(DataType.GUN_DATA, id, json);
                } catch (IOException | JsonSyntaxException | JsonIOException exception) {
                    GunMod.LOGGER.warn(MARKER, "Failed to read data file: {}", file);
                    exception.printStackTrace();
                }
            });
            try {
                Files.walkFileTree(filePath, visitor);
            } catch (IOException e) {
                GunMod.LOGGER.warn(MARKER, "Failed to walk file tree: {}", filePath);
                e.printStackTrace();
            }
        }
    }

    public static void loadFromJsonString(ResourceLocation id, String json) {
        GunData data = CommonGunPackLoader.GSON.fromJson(json, GunData.class);
        // Give every gun a safety fire mode, unless the data pack choose to opt-out
        // by explicitly setting {"safety": false}
        // TODO: Technically speaking, all guns have safeties nowadays.
        // It's just the kind of safety, whether external or internal, matters,
        // In the future this can be added as a enum rather than a boolean, but for now it works
        if (data.getSafety() && ENABLE_GUN_SAFETY.get()) {
            data.getFireModeSet().add(FireMode.SAFETY);
        }
        CommonAssetManager.INSTANCE.putGunData(id, data);
    }
}
