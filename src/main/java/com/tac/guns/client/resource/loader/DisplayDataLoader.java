package com.tac.guns.client.resource.loader;

import com.tac.guns.GunMod;
import com.tac.guns.client.model.BedrockGunModel;
import com.tac.guns.client.resource.pojo.display.GunDisplay;
import com.tac.guns.client.resource.pojo.display.GunModelTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.util.Optional;
import java.util.zip.ZipFile;

public final class DisplayDataLoader {
    private static final Marker MARKER = MarkerManager.getMarker("DisplayDataLoader");
    private static final String DEFAULT_TEXTURE_NAME = "default";

    public static void loadDisplayData(String namespace, String id, String path, ZipFile zipFile, GunDisplay displayData) {
        // 检查默认材质是否存在，并创建默认的RenderType
        Optional<GunModelTexture> defaultOptional = displayData.getModelTextures().stream().filter(texture -> DEFAULT_TEXTURE_NAME.equals(texture.getName())).findAny();
        if (defaultOptional.isEmpty()) {
            GunMod.LOGGER.warn(MARKER, "{} meta file don't have default texture", path);
            return;
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
    }
}
