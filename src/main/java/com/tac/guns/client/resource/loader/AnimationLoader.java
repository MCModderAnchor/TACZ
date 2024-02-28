package com.tac.guns.client.resource.loader;

import com.tac.guns.GunMod;
import com.tac.guns.client.animation.gltf.AnimationStructure;
import com.tac.guns.client.model.BedrockGunModel;
import com.tac.guns.client.resource.cache.ClientAssetManager;
import com.tac.guns.client.resource.pojo.animation.gltf.RawAnimationStructure;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.tac.guns.client.resource.ClientGunLoader.GSON;

public final class AnimationLoader {
    private static final Marker MARKER = MarkerManager.getMarker("AnimationLoader");

    public static void loadAnimation(String namespace, String id, ZipFile zipFile, String path, BedrockGunModel model) {
        String animationPath = String.format("%s/animations/%s", namespace, path);
        ZipEntry modelEntry = zipFile.getEntry(animationPath);
        if (modelEntry == null) {
            GunMod.LOGGER.warn(MARKER, "{} animation file don't exist", animationPath);
            return;
        }
        try (InputStream animationFileStream = zipFile.getInputStream(modelEntry)) {
            RawAnimationStructure rawStructure = GSON.fromJson(new InputStreamReader(animationFileStream, StandardCharsets.UTF_8), RawAnimationStructure.class);
            AnimationStructure structure = new AnimationStructure(rawStructure);
            ClientAssetManager.INSTANCE.setBedrockAnimatedAsset(new ResourceLocation(namespace, id), model, structure);
        } catch (IOException ioe) {
            // 可能用来判定错误，打印下
            GunMod.LOGGER.warn(MARKER, "Failed to load animation: {}", animationPath);
            ioe.printStackTrace();
        }
    }
}
