package com.tac.guns.client.resource;

import com.tac.guns.GunMod;
import com.tac.guns.client.model.bedrock.BedrockModel;
import com.tac.guns.client.resource.pojo.model.BedrockModelPOJO;
import com.tac.guns.client.resource.pojo.model.BedrockVersion;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class InternalAssetLoader {
    public static final ResourceLocation BLACK_MASK_MODEL = new ResourceLocation("tac", "models/others/black_mask_geo.json");
    public static final ResourceLocation BLACK_MASK_TEXTURE = new ResourceLocation("tac", "textures/others/black_mask.png");
    private static final Marker MARKER = MarkerManager.getMarker("BedrockModelLoader");
    private static final Map<ResourceLocation, BedrockModel> models = new HashMap<>();

    public static void onResourceReload() {
        try {
            loadBedrockModel(BLACK_MASK_MODEL);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void loadBedrockModel(ResourceLocation modelLocation) throws IOException {
        Resource resource = Minecraft.getInstance().getResourceManager().getResource(modelLocation);
        try (InputStream stream = resource.getInputStream()) {
            BedrockModelPOJO modelPOJO = ClientGunPackLoader.GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), BedrockModelPOJO.class);
            // 先判断是不是 1.10.0 版本基岩版模型文件
            if (modelPOJO.getFormatVersion().equals(BedrockVersion.LEGACY.getVersion())) {
                // 如果 model 字段不为空
                if (modelPOJO.getGeometryModelLegacy() != null) {
                    models.put(modelLocation, new BedrockModel(modelPOJO, BedrockVersion.LEGACY));
                } else {
                    // 否则日志给出提示
                    GunMod.LOGGER.warn(MARKER, "{} model file don't have model field", modelLocation);
                }
            }
            // 判定是不是 1.12.0 版本基岩版模型文件
            if (modelPOJO.getFormatVersion().equals(BedrockVersion.NEW.getVersion())) {
                // 如果 model 字段不为空
                if (modelPOJO.getGeometryModelNew() != null) {
                    models.put(modelLocation, new BedrockModel(modelPOJO, BedrockVersion.NEW));
                } else {
                    // 否则日志给出提示
                    GunMod.LOGGER.warn(MARKER, "{} model file don't have model field", modelLocation);
                }
            }
            GunMod.LOGGER.warn(MARKER, "{} model version is not 1.10.0 or 1.12.0", modelLocation);
        } catch (IOException ioe) {
            // 可能用来判定错误，打印下
            GunMod.LOGGER.warn(MARKER, "Failed to load model: {}", modelLocation);
            ioe.printStackTrace();
        }
    }

    @Nullable
    public static BedrockModel getBedrockModels(ResourceLocation resourceLocation) {
        return models.get(resourceLocation);
    }
}
