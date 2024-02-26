package com.tac.guns.client.resource.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tac.guns.GunMod;
import com.tac.guns.client.model.BedrockAttachmentModel;
import com.tac.guns.client.model.BedrockGunModel;
import com.tac.guns.client.resource.model.bedrock.BedrockVersion;
import com.tac.guns.client.resource.model.bedrock.pojo.BedrockGunModelPOJO;
import com.tac.guns.client.resource.model.bedrock.pojo.BedrockModelPOJO;
import com.tac.guns.client.resource.model.bedrock.pojo.CubesItem;
import com.tac.guns.client.resource.model.bedrock.pojo.GunModelTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class ModelLoader {
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(ResourceLocation.class, new ResourceLocation.Serializer())
            .registerTypeAdapter(CubesItem.class, new CubesItem.Deserializer())
            .create();

    private static final Marker MARKER = MarkerManager.getMarker("BedrockModelLoader");

    @Nullable
    public static BedrockGunModel loadBedrockGunModel(ResourceLocation metaLocation) throws IOException {
        Resource metaResource = Minecraft.getInstance().getResourceManager().getResource(metaLocation);
        try (InputStream stream = metaResource.getInputStream()) {
            BedrockGunModelPOJO gunModelPOJO = GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), BedrockGunModelPOJO.class);

            //load default texture
            Optional<GunModelTexture> defaultOptional =
                    gunModelPOJO.getTextures().stream().filter(texture -> "default".equals(texture.getName())).findAny();
            if (defaultOptional.isEmpty()) {
                GunMod.LOGGER.warn(MARKER, "{} meta file don't have default texture", metaLocation);
                return null;
            }
            RenderType renderType = RenderType.itemEntityTranslucentCull(defaultOptional.get().getLocation());

            //load model
            ResourceLocation modelLocation = gunModelPOJO.getModelLocation();
            Resource modelResource = Minecraft.getInstance().getResourceManager().getResource(modelLocation);
            try (InputStream stream1 = modelResource.getInputStream()) {
                BedrockModelPOJO pojo = GSON.fromJson(new InputStreamReader(stream1, StandardCharsets.UTF_8), BedrockModelPOJO.class);
                // 先判断是不是 1.10.0 版本基岩版模型文件
                if (pojo.getFormatVersion().equals(BedrockVersion.LEGACY.getVersion())) {
                    // 如果 model 字段不为空
                    if (pojo.getGeometryModelLegacy() != null) {
                        return new BedrockGunModel(pojo, BedrockVersion.LEGACY, renderType);
                    } else {
                        // 否则日志给出提示
                        GunMod.LOGGER.warn(MARKER, "{} model file don't have model field", modelLocation);
                        return null;
                    }
                }

                // 判定是不是 1.12.0 版本基岩版模型文件
                if (pojo.getFormatVersion().equals(BedrockVersion.NEW.getVersion())) {
                    // 如果 model 字段不为空
                    if (pojo.getGeometryModelNew() != null) {
                        return new BedrockGunModel(pojo, BedrockVersion.NEW, renderType);
                    } else {
                        // 否则日志给出提示
                        GunMod.LOGGER.warn(MARKER, "{} model file don't have model field", modelLocation);
                        return null;
                    }
                }

                GunMod.LOGGER.warn(MARKER, "{} model version is not 1.10.0 or 1.12.0", modelLocation);
            } catch (IOException ioe) {
                // 可能用来判定错误，打印下
                GunMod.LOGGER.warn(MARKER, "Failed to load model: {}", modelLocation);
                ioe.printStackTrace();
            }
        }
        // 如果前面出了错，返回 Null
        return null;
    }

    @Nullable
    public static BedrockAttachmentModel loadBedrockAttachmentModel(ResourceLocation modelLocation, ResourceLocation textureLocation) throws IOException {
        Resource resource = Minecraft.getInstance().getResourceManager().getResource(modelLocation);
        //load texture
        RenderType renderType = RenderType.itemEntityTranslucentCull(textureLocation);

        //load model
        try (InputStream stream = resource.getInputStream()) {
            BedrockModelPOJO pojo = GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), BedrockModelPOJO.class);
            // 先判断是不是 1.10.0 版本基岩版模型文件
            if (pojo.getFormatVersion().equals(BedrockVersion.LEGACY.getVersion())) {
                // 如果 model 字段不为空
                if (pojo.getGeometryModelLegacy() != null) {
                    return new BedrockAttachmentModel(pojo, BedrockVersion.LEGACY, renderType);
                } else {
                    // 否则日志给出提示
                    GunMod.LOGGER.warn(MARKER, "{} model file don't have model field", modelLocation);
                    return null;
                }
            }

            // 判定是不是 1.12.0 版本基岩版模型文件
            if (pojo.getFormatVersion().equals(BedrockVersion.NEW.getVersion())) {
                // 如果 model 字段不为空
                if (pojo.getGeometryModelNew() != null) {
                    return new BedrockAttachmentModel(pojo, BedrockVersion.NEW, renderType);
                } else {
                    // 否则日志给出提示
                    GunMod.LOGGER.warn(MARKER, "{} model file don't have model field", modelLocation);
                    return null;
                }
            }

            GunMod.LOGGER.warn(MARKER, "{} model version is not 1.10.0 or 1.12.0", modelLocation);
        } catch (IOException ioe) {
            // 可能用来判定错误，打印下
            GunMod.LOGGER.warn(MARKER, "Failed to load model: {}", modelLocation);
            ioe.printStackTrace();
        }
        // 如果前面出了错，返回 Null
        return null;
    }
}
