package com.tac.guns.client.resource;

import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tac.guns.GunMod;
import com.tac.guns.client.animation.AnimationListenerSupplier;
import com.tac.guns.client.animation.Animations;
import com.tac.guns.client.animation.ObjectAnimation;
import com.tac.guns.client.animation.gltf.AnimationStructure;
import com.tac.guns.client.model.bedrock.BedrockModel;
import com.tac.guns.client.resource.pojo.animation.gltf.RawAnimationStructure;
import com.tac.guns.client.resource.pojo.model.BedrockModelPOJO;
import com.tac.guns.client.resource.pojo.model.BedrockVersion;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InternalAssetLoader {
    private static final ResourceLocation DEFAULT_PISTOL_ANIMATIONS_LOC = new ResourceLocation("tac", "animations/pistol_default.gltf");
    private static final ResourceLocation DEFAULT_RIFLE_ANIMATIONS_LOC = new ResourceLocation("tac", "animations/rifle_default.gltf");

    // 曳光弹模型
    public static final ResourceLocation DEFAULT_BULLET_TEXTURE = new ResourceLocation("tac", "textures/entity/basic_bullet.png");
    public static final ResourceLocation DEFAULT_BULLET_MODEL = new ResourceLocation("tac", "models/bedrock/basic_bullet.json");

    // 射击标靶车
    public static final ResourceLocation TARGET_MINECART_MODEL_LOCATION = new ResourceLocation(GunMod.MOD_ID, "models/bedrock/target_minecart.json");
    public static final ResourceLocation TARGET_MINECART_TEXTURE_LOCATION = new ResourceLocation(GunMod.MOD_ID, "textures/entity/target_minecart.png");
    public static final ResourceLocation ENTITY_EMPTY_TEXTURE = new ResourceLocation(GunMod.MOD_ID, "textures/entity/empty.png");

    // 射击标靶
    public static final ResourceLocation TARGET_MODEL_LOCATION = new ResourceLocation(GunMod.MOD_ID, "models/bedrock/target.json");
    public static final ResourceLocation TARGET_TEXTURE_LOCATION = new ResourceLocation(GunMod.MOD_ID, "textures/block/target.png");
    private static final Map<ResourceLocation, BedrockModel> BEDROCK_MODELS = Maps.newHashMap();

    // 改装台
    public static final ResourceLocation SMITH_TABLE_MODEL_LOCATION = new ResourceLocation(GunMod.MOD_ID, "models/bedrock/gun_smith_table.json");
    public static final ResourceLocation SMITH_TABLE_TEXTURE_LOCATION = new ResourceLocation(GunMod.MOD_ID, "textures/block/gun_smith_table.png");

    private static List<ObjectAnimation> defaultPistolAnimations;
    private static List<ObjectAnimation> defaultRifleAnimations;

    public static void onResourceReload() {
        // 加载默认动画文件
        AnimationStructure pistolAnimationStructure = loadAnimations(DEFAULT_PISTOL_ANIMATIONS_LOC);
        AnimationStructure rifleAnimationStructure = loadAnimations(DEFAULT_RIFLE_ANIMATIONS_LOC);
        defaultPistolAnimations = Animations.createAnimationFromGltf(pistolAnimationStructure, (AnimationListenerSupplier[]) null);
        defaultRifleAnimations = Animations.createAnimationFromGltf(rifleAnimationStructure, (AnimationListenerSupplier[]) null);

        // 加载代码直接调用的基岩版模型
        BEDROCK_MODELS.clear();
        loadBedrockModels(InternalAssetLoader.SMITH_TABLE_MODEL_LOCATION);
        loadBedrockModels(InternalAssetLoader.TARGET_MODEL_LOCATION);
        loadBedrockModels(InternalAssetLoader.TARGET_MINECART_MODEL_LOCATION);
        loadBedrockModels(InternalAssetLoader.DEFAULT_BULLET_MODEL);
    }

    private static AnimationStructure loadAnimations(ResourceLocation resourceLocation) {
        try {
            Resource resource = Minecraft.getInstance().getResourceManager().getResource(resourceLocation);
            InputStream inputStream = resource.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            JsonObject json = JsonParser.parseReader(bufferedReader).getAsJsonObject();
            RawAnimationStructure rawAnimationStructure = ClientGunPackLoader.GSON.fromJson(json, RawAnimationStructure.class);
            return new AnimationStructure(rawAnimationStructure);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void loadBedrockModels(ResourceLocation location) {
        try (InputStream stream = Minecraft.getInstance().getResourceManager().getResource(location).getInputStream()) {
            BedrockModelPOJO pojo = ClientGunPackLoader.GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), BedrockModelPOJO.class);
            BEDROCK_MODELS.put(location, new BedrockModel(pojo, BedrockVersion.NEW));
        } catch (IOException ioException) {
            ioException.fillInStackTrace();
        }
    }

    public static List<ObjectAnimation> getDefaultPistolAnimations() {
        return defaultPistolAnimations;
    }

    public static List<ObjectAnimation> getDefaultRifleAnimations() {
        return defaultRifleAnimations;
    }

    public static Optional<BedrockModel> getBedrockModel(ResourceLocation location) {
        return Optional.ofNullable(BEDROCK_MODELS.get(location));
    }
}
