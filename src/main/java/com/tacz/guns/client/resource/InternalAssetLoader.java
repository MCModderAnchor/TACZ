package com.tacz.guns.client.resource;

import com.google.common.collect.Maps;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.tacz.guns.GunMod;
import com.tacz.guns.client.animation.Animations;
import com.tacz.guns.client.animation.ObjectAnimation;
import com.tacz.guns.client.model.bedrock.BedrockModel;
import com.tacz.guns.client.resource.pojo.animation.bedrock.BedrockAnimationFile;
import com.tacz.guns.client.resource.pojo.model.BedrockModelPOJO;
import com.tacz.guns.client.resource.pojo.model.BedrockVersion;
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
    // 曳光弹模型
    public static final ResourceLocation DEFAULT_BULLET_TEXTURE = new ResourceLocation(GunMod.MOD_ID, "textures/entity/basic_bullet.png");
    public static final ResourceLocation DEFAULT_BULLET_MODEL = new ResourceLocation(GunMod.MOD_ID, "models/bedrock/basic_bullet.json");
    // 射击标靶车
    public static final ResourceLocation TARGET_MINECART_MODEL_LOCATION = new ResourceLocation(GunMod.MOD_ID, "models/bedrock/target_minecart.json");
    public static final ResourceLocation TARGET_MINECART_TEXTURE_LOCATION = new ResourceLocation(GunMod.MOD_ID, "textures/entity/target_minecart.png");
    public static final ResourceLocation ENTITY_EMPTY_TEXTURE = new ResourceLocation(GunMod.MOD_ID, "textures/entity/empty.png");
    // 射击标靶
    public static final ResourceLocation TARGET_MODEL_LOCATION = new ResourceLocation(GunMod.MOD_ID, "models/bedrock/target.json");
    public static final ResourceLocation TARGET_TEXTURE_LOCATION = new ResourceLocation(GunMod.MOD_ID, "textures/block/target.png");
    // 雕像
    public static final ResourceLocation STATUE_MODEL_LOCATION = new ResourceLocation(GunMod.MOD_ID, "models/bedrock/statue.json");
    public static final ResourceLocation STATUE_TEXTURE_LOCATION = new ResourceLocation(GunMod.MOD_ID, "textures/block/statue.png");
    // 改装台
    public static final ResourceLocation SMITH_TABLE_MODEL_LOCATION = new ResourceLocation(GunMod.MOD_ID, "models/bedrock/gun_smith_table.json");
    public static final ResourceLocation SMITH_TABLE_TEXTURE_LOCATION = new ResourceLocation(GunMod.MOD_ID, "textures/block/gun_smith_table.png");
    // 默认动画
    private static final ResourceLocation DEFAULT_PISTOL_ANIMATIONS_LOC = new ResourceLocation(GunMod.MOD_ID, "animations/pistol_default.animation.json");
    private static final ResourceLocation DEFAULT_RIFLE_ANIMATIONS_LOC = new ResourceLocation(GunMod.MOD_ID, "animations/rifle_default.animation.json");
    // 内部资源缓存
    private static final Map<ResourceLocation, BedrockModel> BEDROCK_MODELS = Maps.newHashMap();
    private static List<ObjectAnimation> defaultPistolAnimations;
    private static List<ObjectAnimation> defaultRifleAnimations;

    public static void onResourceReload() {
        // 加载默认动画文件
        BedrockAnimationFile pistolAnimationFile = loadAnimations(DEFAULT_PISTOL_ANIMATIONS_LOC);
        BedrockAnimationFile rifleAnimationFile = loadAnimations(DEFAULT_RIFLE_ANIMATIONS_LOC);
        defaultPistolAnimations = Animations.createAnimationFromBedrock(pistolAnimationFile);
        defaultRifleAnimations = Animations.createAnimationFromBedrock(rifleAnimationFile);

        // 加载代码直接调用的基岩版模型
        BEDROCK_MODELS.clear();
        loadBedrockModels(InternalAssetLoader.SMITH_TABLE_MODEL_LOCATION);
        loadBedrockModels(InternalAssetLoader.TARGET_MODEL_LOCATION);
        loadBedrockModels(InternalAssetLoader.TARGET_MINECART_MODEL_LOCATION);
        loadBedrockModels(InternalAssetLoader.DEFAULT_BULLET_MODEL);
        loadBedrockModels(InternalAssetLoader.STATUE_MODEL_LOCATION);
    }

    private static BedrockAnimationFile loadAnimations(ResourceLocation resourceLocation) {
        try {
            Resource resource = Minecraft.getInstance().getResourceManager().getResource(resourceLocation);
            InputStream inputStream = resource.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            JsonObject json = JsonParser.parseReader(bufferedReader).getAsJsonObject();
            return ClientGunPackLoader.GSON.fromJson(json, BedrockAnimationFile.class);
        } catch (IOException | JsonSyntaxException | JsonIOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void loadBedrockModels(ResourceLocation location) {
        try (InputStream stream = Minecraft.getInstance().getResourceManager().getResource(location).getInputStream()) {
            BedrockModelPOJO pojo = ClientGunPackLoader.GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), BedrockModelPOJO.class);
            BEDROCK_MODELS.put(location, new BedrockModel(pojo, BedrockVersion.NEW));
        } catch (IOException | JsonSyntaxException | JsonIOException e) {
            e.fillInStackTrace();
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