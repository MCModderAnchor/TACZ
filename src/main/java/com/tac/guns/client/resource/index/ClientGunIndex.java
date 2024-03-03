package com.tac.guns.client.resource.index;

import com.google.common.collect.Maps;
import com.tac.guns.client.animation.AnimationController;
import com.tac.guns.client.animation.Animations;
import com.tac.guns.client.animation.gltf.AnimationStructure;
import com.tac.guns.client.animation.internal.GunAnimationStateMachine;
import com.tac.guns.client.model.BedrockGunModel;
import com.tac.guns.client.resource.ClientAssetManager;
import com.tac.guns.resource.pojo.GunIndexPOJO;
import com.tac.guns.client.resource.pojo.display.GunDisplay;
import com.tac.guns.client.resource.pojo.display.GunModelTexture;
import com.tac.guns.client.resource.pojo.display.GunTransform;
import com.tac.guns.client.resource.pojo.model.BedrockModelPOJO;
import com.tac.guns.client.resource.pojo.model.BedrockVersion;
import com.tac.guns.item.nbt.GunItemData;
import com.tac.guns.resource.CommonAssetManager;
import com.tac.guns.resource.pojo.data.GunData;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class ClientGunIndex {
    private static final String DEFAULT_TEXTURE_NAME = "default";
    private String name;
    @Nullable
    private String tooltip;
    private BedrockGunModel gunModel;
    private GunAnimationStateMachine animationStateMachine;
    private Map<String, ResourceLocation> sounds;
    private GunTransform transform;
    private GunData gunData;
    private RenderType slotRenderType;

    public static ClientGunIndex getInstance(GunIndexPOJO clientPojo) throws IllegalArgumentException {
        ClientGunIndex index = new ClientGunIndex();

        GunDisplay display = checkDisplay(clientPojo);

        checkData(clientPojo, index);

        checkName(clientPojo, index);
        checkTooltip(clientPojo, index);
        checkTextureAndModel(display, index);
        checkSlotTexture(display, index);
        checkAnimation(display, index);
        checkSounds(display, index);
        checkTransform(display, index);

        return index;
    }

    private static void checkName(GunIndexPOJO gunIndexPOJO, ClientGunIndex index) {
        index.name = gunIndexPOJO.getName();
        if (StringUtils.isBlank(index.name)) {
            index.name = "custom.tac.gun.error.no_name";
        }
    }

    private static void checkTooltip(GunIndexPOJO gunIndexPOJO, ClientGunIndex index) {
        index.tooltip = gunIndexPOJO.getTooltip();
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

        // 检查材质
        List<GunModelTexture> textures = getAndCheckTextures(display);
        // 检查默认材质是否存在
        Optional<GunModelTexture> defaultOptional = textures.stream().filter(texture -> DEFAULT_TEXTURE_NAME.equals(texture.getId())).findAny();
        if (defaultOptional.isEmpty()) {
            throw new IllegalArgumentException("there is no default texture, please add a default texture");
        }
        // 创建默认的 RenderType
        RenderType renderType = RenderType.itemEntityTranslucentCull(defaultOptional.get().getLocation());
        // 先判断是不是 1.10.0 版本基岩版模型文件
        if (modelPOJO.getFormatVersion().equals(BedrockVersion.LEGACY.getVersion()) && modelPOJO.getGeometryModelLegacy() != null) {
            index.gunModel = new BedrockGunModel(modelPOJO, BedrockVersion.LEGACY, renderType);
        }
        // 判定是不是 1.12.0 版本基岩版模型文件
        if (modelPOJO.getFormatVersion().equals(BedrockVersion.NEW.getVersion()) && modelPOJO.getGeometryModelNew() != null) {
            index.gunModel = new BedrockGunModel(modelPOJO, BedrockVersion.NEW, renderType);
        }

        if (index.gunModel == null) {
            throw new IllegalArgumentException("there is no model data in the model file");
        }
    }

    @NotNull
    private static List<GunModelTexture> getAndCheckTextures(GunDisplay display) {
        List<GunModelTexture> textures = display.getModelTextures();
        if (textures == null) {
            throw new IllegalArgumentException("display object missing textures field");
        }
        if (textures.isEmpty()) {
            throw new IllegalArgumentException("display object's textures field is empty");
        }
        textures.forEach(t -> {
            if (StringUtils.isBlank(t.getId())) {
                throw new IllegalArgumentException("textures missing id field");
            }
            if (t.getLocation() == null) {
                throw new IllegalArgumentException("textures missing location field");
            }
        });
        return textures;
    }

    private static void checkAnimation(GunDisplay display, ClientGunIndex index) {
        ResourceLocation location = display.getAnimationLocation();
        if (location == null) {
            location = GunItemData.DEFAULT;
        }
        // 目前支持的动画为 gltf 动画。此处从缓存取出 gltf 的动画资源。
        AnimationStructure animations = ClientAssetManager.INSTANCE.getAnimations(location);
        if (animations == null) {
            animations = Objects.requireNonNull(ClientAssetManager.INSTANCE.getAnimations(GunItemData.DEFAULT));
        }
        // 用 gltf 动画资源创建动画控制器
        AnimationController controller = Animations.createControllerFromGltf(animations, index.gunModel);
        // 将动画控制器包装起来
        index.animationStateMachine = new GunAnimationStateMachine(controller);
    }

    private static void checkSounds(GunDisplay display, ClientGunIndex index) {
        Map<String, ResourceLocation> soundMaps = display.getSounds();
        GunDisplay defaultDisplay = ClientAssetManager.INSTANCE.getGunDisplay(GunItemData.DEFAULT_DISPLAY);
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
        GunDisplay defaultDisplay = ClientAssetManager.INSTANCE.getGunDisplay(GunItemData.DEFAULT_DISPLAY);
        if (readTransform == null || readTransform.getScale() == null) {
            index.transform = Objects.requireNonNull(defaultDisplay.getTransform());
        } else {
            index.transform = display.getTransform();
        }
    }

    private static void checkSlotTexture(GunDisplay display, ClientGunIndex index) {
        // 加载 GUI 内枪械图标
        ResourceLocation slotTexture = Objects.requireNonNullElseGet(display.getSlotTextureLocation(), MissingTextureAtlasSprite::getLocation);
        index.slotRenderType = RenderType.entityTranslucent(slotTexture);
    }

    private ClientGunIndex() {
    }

    public String getName() {
        return name;
    }

    @Nullable
    public String getTooltip() {
        return tooltip;
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

    public RenderType getSlotRenderType() {
        return slotRenderType;
    }

    public GunData getGunData() {
        return gunData;
    }
}