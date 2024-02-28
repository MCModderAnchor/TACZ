package com.tac.guns.client.resource.loader;

import com.tac.guns.GunMod;
import com.tac.guns.client.model.BedrockGunModel;
import com.tac.guns.client.resource.pojo.model.BedrockModelPOJO;
import com.tac.guns.client.resource.pojo.model.BedrockVersion;
import net.minecraft.client.renderer.RenderType;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.tac.guns.client.resource.ClientGunLoader.GSON;

public final class BedrockModelLoader {
    private static final Marker MARKER = MarkerManager.getMarker("BedrockModelLoader");

    public static BedrockGunModel loadGunModel(String namespace, String id, ZipFile zipFile, String path, RenderType renderType) {
        String modelPath = String.format("%s/models/%s", namespace, path);
        ZipEntry modelEntry = zipFile.getEntry(modelPath);

        if (modelEntry == null) {
            GunMod.LOGGER.warn(MARKER, "{} model file don't exist", modelPath);
            return null;
        }

        try (InputStream modelFileStream = zipFile.getInputStream(modelEntry)) {
            BedrockModelPOJO pojo = GSON.fromJson(new InputStreamReader(modelFileStream, StandardCharsets.UTF_8), BedrockModelPOJO.class);
            // 先判断是不是 1.10.0 版本基岩版模型文件
            if (pojo.getFormatVersion().equals(BedrockVersion.LEGACY.getVersion())) {
                // 如果 model 字段不为空
                if (pojo.getGeometryModelLegacy() != null) {
                    return new BedrockGunModel(pojo, BedrockVersion.LEGACY, renderType);
                } else {
                    // 否则日志给出提示
                    GunMod.LOGGER.warn(MARKER, "{} model file don't have model field", modelPath);
                }
                return null;
            }

            // 判定是不是 1.12.0 版本基岩版模型文件
            if (pojo.getFormatVersion().equals(BedrockVersion.NEW.getVersion())) {
                // 如果 model 字段不为空
                if (pojo.getGeometryModelNew() != null) {
                    return new BedrockGunModel(pojo, BedrockVersion.NEW, renderType);
                } else {
                    // 否则日志给出提示
                    GunMod.LOGGER.warn(MARKER, "{} model file don't have model field", modelPath);
                }
                return null;
            }

            GunMod.LOGGER.warn(MARKER, "{} model version is not 1.10.0 or 1.12.0", modelPath);
        } catch (IOException ioe) {
            // 可能用来判定错误，打印下
            GunMod.LOGGER.warn(MARKER, "Failed to load model: {}", modelPath);
            ioe.printStackTrace();
        }
        return null;
    }
}
