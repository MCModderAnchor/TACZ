package com.tacz.guns.resource_legacy.loader.index;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.tacz.guns.GunMod;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource_legacy.network.CommonGunPackNetwork;
import com.tacz.guns.resource.network.DataType;
import com.tacz.guns.resource.pojo.GunIndexPOJO;
import com.tacz.guns.util.TacPathVisitor;
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

import static com.tacz.guns.resource_legacy.CommonGunPackLoader.GSON;
import static com.tacz.guns.resource_legacy.CommonGunPackLoader.GUN_INDEX;

public final class CommonGunIndexLoader {
    private static final Pattern GUNS_INDEX_PATTERN = Pattern.compile("^(\\w+)/guns/index/(\\w+)\\.json$");
    private static final Marker MARKER = MarkerManager.getMarker("CommonGunIndexLoader");

    public static void loadGunIndex(String path, ZipFile zipFile) throws IOException {
        Matcher matcher = GUNS_INDEX_PATTERN.matcher(path);
        if (matcher.find()) {
            String namespace = matcher.group(1);
            String id = matcher.group(2);
            ZipEntry entry = zipFile.getEntry(path);
            if (entry == null) {
                GunMod.LOGGER.warn(MARKER, "{} file don't exist", path);
                return;
            }
            try (InputStream stream = zipFile.getInputStream(entry)) {
                String json = IOUtils.toString(stream, StandardCharsets.UTF_8);
                ResourceLocation registryName = new ResourceLocation(namespace, id);
                loadGunFromJsonString(registryName, json);
                CommonGunPackNetwork.addData(DataType.GUN_INDEX, registryName, json);
            } catch (IllegalArgumentException | JsonSyntaxException | JsonIOException exception) {
                GunMod.LOGGER.warn("{} index file read fail!", path);
                exception.printStackTrace();
            }
        }
    }

    public static void loadGunIndex(File root) throws IOException {
        Path filePath = root.toPath().resolve("guns/index");
        if (Files.isDirectory(filePath)) {
            TacPathVisitor visitor = new TacPathVisitor(filePath.toFile(), root.getName(), ".json", (id, file) -> {
                try (InputStream stream = Files.newInputStream(file)) {
                    String json = IOUtils.toString(stream, StandardCharsets.UTF_8);
                    loadGunFromJsonString(id, json);
                    CommonGunPackNetwork.addData(DataType.GUN_INDEX, id, json);
                } catch (IllegalArgumentException | IOException | JsonSyntaxException | JsonIOException exception) {
                    GunMod.LOGGER.warn("{} index file read fail!", file);
                    exception.printStackTrace();
                }
            });
            Files.walkFileTree(filePath, visitor);
        }
    }

    public static void loadGunFromJsonString(ResourceLocation id, String json) {
        GunIndexPOJO indexPOJO = GSON.fromJson(json, GunIndexPOJO.class);
        GUN_INDEX.put(id, CommonGunIndex.getInstance(indexPOJO));
    }
}
