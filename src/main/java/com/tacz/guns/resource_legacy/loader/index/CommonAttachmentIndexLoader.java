package com.tacz.guns.resource_legacy.loader.index;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.tacz.guns.GunMod;
import com.tacz.guns.resource.index.CommonAttachmentIndex;
import com.tacz.guns.resource_legacy.network.CommonGunPackNetwork;
import com.tacz.guns.resource.network.DataType;
import com.tacz.guns.resource.pojo.AttachmentIndexPOJO;
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

import static com.tacz.guns.resource_legacy.CommonGunPackLoader.ATTACHMENT_INDEX;
import static com.tacz.guns.resource_legacy.CommonGunPackLoader.GSON;

public final class CommonAttachmentIndexLoader {
    private static final Pattern ATTACHMENT_INDEX_PATTERN = Pattern.compile("^(\\w+)/attachments/index/(\\w+)\\.json$");
    private static final Marker MARKER = MarkerManager.getMarker("CommonAttachmentIndexLoader");

    public static void loadAttachmentIndex(String path, ZipFile zipFile) throws IOException {
        Matcher matcher = ATTACHMENT_INDEX_PATTERN.matcher(path);
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
                loadAttachmentFromJsonString(registryName, json);
                CommonGunPackNetwork.addData(DataType.ATTACHMENT_INDEX, registryName, json);
            } catch (IllegalArgumentException | JsonSyntaxException | JsonIOException exception) {
                GunMod.LOGGER.warn("{} index file read fail!", path);
                exception.printStackTrace();
            }
        }
    }

    public static void loadAttachmentIndex(File root) throws IOException {
        Path filePath = root.toPath().resolve("attachments/index");
        if (Files.isDirectory(filePath)) {
            TacPathVisitor visitor = new TacPathVisitor(filePath.toFile(), root.getName(), ".json", (id, file) -> {
                try (InputStream stream = Files.newInputStream(file)) {
                    String json = IOUtils.toString(stream, StandardCharsets.UTF_8);
                    loadAttachmentFromJsonString(id, json);
                    CommonGunPackNetwork.addData(DataType.ATTACHMENT_INDEX, id, json);
                } catch (IllegalArgumentException | IOException | JsonSyntaxException | JsonIOException exception) {
                    GunMod.LOGGER.warn("{} index file read fail!", file);
                    exception.printStackTrace();
                }
            });
            Files.walkFileTree(filePath, visitor);
        }
    }

    public static void loadAttachmentFromJsonString(ResourceLocation id, String json) {
        AttachmentIndexPOJO indexPOJO = GSON.fromJson(json, AttachmentIndexPOJO.class);
        ATTACHMENT_INDEX.put(id, CommonAttachmentIndex.getInstance(indexPOJO));
    }
}
