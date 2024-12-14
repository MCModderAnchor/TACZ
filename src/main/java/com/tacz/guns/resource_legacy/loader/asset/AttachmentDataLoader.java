package com.tacz.guns.resource_legacy.loader.asset;

import com.google.gson.*;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.modifier.JsonProperty;
import com.tacz.guns.resource_legacy.CommonAssetManager;
import com.tacz.guns.resource_legacy.CommonGunPackLoader;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import com.tacz.guns.resource_legacy.network.CommonGunPackNetwork;
import com.tacz.guns.resource.network.DataType;
import com.tacz.guns.resource.pojo.data.attachment.AttachmentData;
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

public final class AttachmentDataLoader {
    private static final Marker MARKER = MarkerManager.getMarker("AttachmentDataLoader");
    private static final Pattern DATA_PATTERN = Pattern.compile("^(\\w+)/attachments/data/([\\w/]+)\\.json$");

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
                CommonGunPackNetwork.addData(DataType.ATTACHMENT_DATA, registryName, json);
                return true;
            } catch (IOException | JsonSyntaxException | JsonIOException exception) {
                GunMod.LOGGER.warn(MARKER, "Failed to read data file: {}, entry: {}", zipFile, entry);
                exception.printStackTrace();
            }
        }
        return false;
    }

    public static void load(File root) {
        Path filePath = root.toPath().resolve("attachments/data");
        if (Files.isDirectory(filePath)) {
            TacPathVisitor visitor = new TacPathVisitor(filePath.toFile(), root.getName(), ".json", (id, file) -> {
                try (InputStream stream = Files.newInputStream(file)) {
                    String json = IOUtils.toString(stream, StandardCharsets.UTF_8);
                    loadFromJsonString(id, json);
                    CommonGunPackNetwork.addData(DataType.ATTACHMENT_DATA, id, json);
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
        AttachmentData data = CommonGunPackLoader.GSON.fromJson(json, AttachmentData.class);
        // 序列化注册的配件属性修改
        AttachmentPropertyManager.getModifiers().forEach((key, value) -> {
            JsonElement jsonElement = JsonParser.parseString(json);
            if (!jsonElement.isJsonObject()) {
                return;
            }
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            if (jsonObject.has(key)) {
                JsonProperty<?> property = value.readJson(json);
                property.initComponents();
                data.addModifier(key, property);
            } else if (jsonObject.has(value.getOptionalFields())) {
                // 为了兼容旧版本，读取可选字段名
                JsonProperty<?> property = value.readJson(json);
                property.initComponents();
                data.addModifier(key, property);
            }
        });
        CommonAssetManager.INSTANCE.putAttachmentData(id, data);
    }
}
