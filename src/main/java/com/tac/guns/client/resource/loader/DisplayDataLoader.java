package com.tac.guns.client.resource.loader;

import com.tac.guns.GunMod;
import com.tac.guns.client.model.BedrockGunModel;
import com.tac.guns.client.resource.ClientGunLoader;
import com.tac.guns.client.resource.pojo.display.GunDisplay;
import com.tac.guns.client.resource.pojo.display.GunModelTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class DisplayDataLoader {
    private static final Marker MARKER = MarkerManager.getMarker("DisplayDataLoader");
    private static final String DEFAULT_TEXTURE_NAME = "default";

    @Nullable
    public static GunDisplay loadDisplayData(String namespace, String id, ZipFile zipFile) throws IOException {
        String filePath = String.format("%s/guns/display/%s.display.json", namespace, id);
        ZipEntry entry = zipFile.getEntry(filePath);
        if (entry == null) {
            GunMod.LOGGER.warn(MARKER, "{} file don't exist", filePath);
            return null;
        }
        try (InputStream stream = zipFile.getInputStream(entry)) {
            // 获取枪械的 display 文件
            GunDisplay displayData = ClientGunLoader.GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), GunDisplay.class);
            // 检查默认材质是否存在，并创建默认的RenderType
            Optional<GunModelTexture> defaultOptional = displayData.getModelTextures().stream().filter(texture -> DEFAULT_TEXTURE_NAME.equals(texture.getId())).findAny();
            if (defaultOptional.isEmpty()) {
                GunMod.LOGGER.warn(MARKER, "{} display file don't have default texture", filePath);
                return null;
            }
            ResourceLocation defaultTextureRegistry = TextureLoader.loadTexture(zipFile.getName(), new ResourceLocation(namespace, defaultOptional.get().getLocation()));
            RenderType renderType = RenderType.itemEntityTranslucentCull(defaultTextureRegistry);
            // 注册所有需要的材质
            displayData.getModelTextures().forEach(texture -> {
                ResourceLocation registryName = new ResourceLocation(namespace, texture.getLocation());
                ResourceLocation textureName = TextureLoader.loadTexture(zipFile.getName(), registryName);
                // todo ClientAssetManager.INSTANCE.setZipTextures(registryName, textureName);
            });
            // 加载模型
            BedrockGunModel model = BedrockModelLoader.loadGunModel(namespace, id, zipFile, displayData.getModelLocation(), renderType);
            // 加载动画
            if (model != null && displayData.getAnimationLocation() != null) {
                AnimationLoader.loadAnimation(namespace, id, zipFile, displayData.getAnimationLocation(), model);
            }
            return displayData;
        }
    }
}
