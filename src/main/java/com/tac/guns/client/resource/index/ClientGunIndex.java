package com.tac.guns.client.resource.index;

import com.google.common.collect.Maps;
import com.tac.guns.client.animation.AnimationController;
import com.tac.guns.client.animation.Animations;
import com.tac.guns.client.animation.gltf.AnimationStructure;
import com.tac.guns.client.animation.internal.GunAnimationStateMachine;
import com.tac.guns.client.model.BedrockGunModel;
import com.tac.guns.client.resource.ClientAssetManager;
import com.tac.guns.client.resource.pojo.display.gun.AnimationInfluenceCoefficient;
import com.tac.guns.client.resource.pojo.display.gun.GunDisplay;
import com.tac.guns.client.resource.pojo.display.gun.GunTransform;
import com.tac.guns.client.resource.pojo.model.BedrockModelPOJO;
import com.tac.guns.client.resource.pojo.model.BedrockVersion;
import com.tac.guns.resource.CommonAssetManager;
import com.tac.guns.resource.DefaultAssets;
import com.tac.guns.resource.pojo.GunIndexPOJO;
import com.tac.guns.resource.pojo.data.gun.GunData;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;

@OnlyIn(Dist.CLIENT)
public class ClientGunIndex {
    private String name;
    private String thirdPersonAnimation = "empty";
    private BedrockGunModel gunModel;
    private GunAnimationStateMachine animationStateMachine;
    private Map<String, ResourceLocation> sounds;
    private GunTransform transform;
    private GunData gunData;
    private AnimationInfluenceCoefficient animationInfluenceCoefficient;
    private ResourceLocation modelTexture;
    private ResourceLocation slotTexture;
    private ResourceLocation hudTexture;
    private String type;

    private ClientGunIndex() {
    }

    public static ClientGunIndex getInstance(GunIndexPOJO gunIndexPOJO) throws IllegalArgumentException {
        ClientGunIndex index = new ClientGunIndex();
        checkIndex(gunIndexPOJO, index);
        GunDisplay display = checkDisplay(gunIndexPOJO);
        checkData(gunIndexPOJO, index);
        checkName(gunIndexPOJO, index);
        checkTextureAndModel(display, index);
        checkSlotTexture(display, index);
        checkHUDTexture(display, index);
        checkAnimation(display, index);
        checkSounds(display, index);
        checkTransform(display, index);
        checkAnimationInfluenceCoefficient(display, index);
        return index;
    }

    private static void checkIndex(GunIndexPOJO gunIndexPOJO, ClientGunIndex index) {
        if (gunIndexPOJO == null) {
            throw new IllegalArgumentException("index object file is empty");
        }
        if (StringUtils.isBlank(gunIndexPOJO.getType())) {
            throw new IllegalArgumentException("index object missing type field");
        }
        index.type = gunIndexPOJO.getType();
    }

    private static void checkName(GunIndexPOJO gunIndexPOJO, ClientGunIndex index) {
        index.name = gunIndexPOJO.getName();
        if (StringUtils.isBlank(index.name)) {
            index.name = "custom.tac.error.no_name";
        }
    }

    private static void checkData(GunIndexPOJO gunIndexPOJO, ClientGunIndex index) {
        ResourceLocation pojoData = gunIndexPOJO.getData();
        if (pojoData == null) {
            throw new IllegalArgumentException("index object missing pojoData field");
        }
        GunData data = CommonAssetManager.INSTANCE.getGunData(pojoData);
        if (data == null) {
            throw new IllegalArgumentException("there is no corresponding data file");
        }
        // 剩下的不需要校验了，Common的读取逻辑中已经校验过了
        index.gunData = data;
    }

    @NotNull
    private static GunDisplay checkDisplay(GunIndexPOJO gunIndexPOJO) {
        ResourceLocation pojoDisplay = gunIndexPOJO.getDisplay();
        if (pojoDisplay == null) {
            throw new IllegalArgumentException("index object missing display field");
        }
        GunDisplay display = ClientAssetManager.INSTANCE.getGunDisplay(pojoDisplay);
        if (display == null) {
            throw new IllegalArgumentException("there is no corresponding display file");
        }
        return display;
    }

    private static void checkTextureAndModel(GunDisplay display, ClientGunIndex index) {
        // 检查模型
        ResourceLocation modelLocation = display.getModelLocation();
        if (modelLocation == null) {
            throw new IllegalArgumentException("display object missing model field");
        }
        BedrockModelPOJO modelPOJO = ClientAssetManager.INSTANCE.getModels(modelLocation);
        if (modelPOJO == null) {
            throw new IllegalArgumentException("there is no corresponding model file");
        }
        // 检查默认材质是否存在
        ResourceLocation textureLocation = display.getModelTexture();
        if (textureLocation == null) {
            throw new IllegalArgumentException("missing default texture");
        }
        index.modelTexture = textureLocation;
        // 先判断是不是 1.10.0 版本基岩版模型文件
        if (modelPOJO.getFormatVersion().equals(BedrockVersion.LEGACY.getVersion()) && modelPOJO.getGeometryModelLegacy() != null) {
            index.gunModel = new BedrockGunModel(modelPOJO, BedrockVersion.LEGACY);
        }
        // 判定是不是 1.12.0 版本基岩版模型文件
        if (modelPOJO.getFormatVersion().equals(BedrockVersion.NEW.getVersion()) && modelPOJO.getGeometryModelNew() != null) {
            index.gunModel = new BedrockGunModel(modelPOJO, BedrockVersion.NEW);
        }

        if (index.gunModel == null) {
            throw new IllegalArgumentException("there is no model data in the model file");
        }
    }

    private static void checkAnimation(GunDisplay display, ClientGunIndex index) {
        ResourceLocation location = display.getAnimationLocation();
        if (location == null) {
            location = DefaultAssets.DEFAULT_GUN_ID;
        }
        // 目前支持的动画为 gltf 动画。此处从缓存取出 gltf 的动画资源。
        AnimationStructure animations = ClientAssetManager.INSTANCE.getAnimations(location);
        if (animations == null) {
            animations = Objects.requireNonNull(ClientAssetManager.INSTANCE.getAnimations(DefaultAssets.DEFAULT_GUN_ID));
        }
        // 用 gltf 动画资源创建动画控制器
        AnimationController controller = Animations.createControllerFromGltf(animations, index.gunModel);
        // 将动画控制器包装起来
        index.animationStateMachine = new GunAnimationStateMachine(controller);
        if (StringUtils.isNoneBlank(display.getThirdPersonAnimation())) {
            index.thirdPersonAnimation = display.getThirdPersonAnimation();
        }
    }

    private static void checkSounds(GunDisplay display, ClientGunIndex index) {
        Map<String, ResourceLocation> soundMaps = display.getSounds();
        GunDisplay defaultDisplay = ClientAssetManager.INSTANCE.getGunDisplay(DefaultAssets.DEFAULT_GUN_DISPLAY);
        Map<String, ResourceLocation> defaultSoundMaps = Objects.requireNonNull(defaultDisplay.getSounds());
        if (soundMaps == null || soundMaps.isEmpty()) {
            index.sounds = defaultSoundMaps;
            return;
        }
        index.sounds = Maps.newHashMap();
        for (String name : defaultSoundMaps.keySet()) {
            if (soundMaps.containsKey(name)) {
                index.sounds.put(name, soundMaps.get(name));
            } else {
                index.sounds.put(name, defaultSoundMaps.get(name));
            }
        }
    }

    private static void checkTransform(GunDisplay display, ClientGunIndex index) {
        GunTransform readTransform = display.getTransform();
        GunDisplay defaultDisplay = ClientAssetManager.INSTANCE.getGunDisplay(DefaultAssets.DEFAULT_GUN_DISPLAY);
        if (readTransform == null || readTransform.getScale() == null) {
            index.transform = Objects.requireNonNull(defaultDisplay.getTransform());
        } else {
            index.transform = display.getTransform();
        }
    }

    private static void checkSlotTexture(GunDisplay display, ClientGunIndex index) {
        // 加载 GUI 内枪械图标
        index.slotTexture = Objects.requireNonNullElseGet(display.getSlotTextureLocation(), MissingTextureAtlasSprite::getLocation);
    }

    private static void checkAnimationInfluenceCoefficient(GunDisplay display, ClientGunIndex index) {
        index.animationInfluenceCoefficient = Objects.requireNonNullElse(display.getAnimationInfluenceCoefficient(), new AnimationInfluenceCoefficient());
        index.animationInfluenceCoefficient.writeDefaultIfNull();
    }

    private static void checkHUDTexture(GunDisplay display, ClientGunIndex index) {
        index.hudTexture = Objects.requireNonNullElseGet(display.getHudTextureLocation(), MissingTextureAtlasSprite::getLocation);
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public BedrockGunModel getGunModel() {
        return gunModel;
    }

    public GunAnimationStateMachine getAnimationStateMachine() {
        return animationStateMachine;
    }

    public ResourceLocation getSounds(String name) {
        return sounds.get(name);
    }

    public GunTransform getTransform() {
        return transform;
    }

    public ResourceLocation getSlotTexture() {
        return slotTexture;
    }

    public ResourceLocation getHUDTexture() {
        return hudTexture;
    }

    public ResourceLocation getModelTexture() {
        return modelTexture;
    }

    public GunData getGunData() {
        return gunData;
    }

    public AnimationInfluenceCoefficient getAnimationInfluenceCoefficient() {
        return animationInfluenceCoefficient;
    }

    public String getThirdPersonAnimation() {
        return thirdPersonAnimation;
    }
}