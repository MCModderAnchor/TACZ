package com.tacz.guns.client.resource.index;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.client.animation.AnimationController;
import com.tacz.guns.api.client.animation.Animations;
import com.tacz.guns.api.client.animation.ObjectAnimation;
import com.tacz.guns.api.client.animation.gltf.AnimationStructure;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.client.animation.statemachine.GunAnimationStateMachine;
import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.resource.ClientAssetManager;
import com.tacz.guns.client.resource.InternalAssetLoader;
import com.tacz.guns.client.resource.pojo.animation.bedrock.BedrockAnimationFile;
import com.tacz.guns.client.resource.pojo.display.ammo.AmmoParticle;
import com.tacz.guns.client.resource.pojo.display.gun.*;
import com.tacz.guns.client.resource.pojo.model.BedrockModelPOJO;
import com.tacz.guns.client.resource.pojo.model.BedrockVersion;
import com.tacz.guns.resource.CommonAssetManager;
import com.tacz.guns.resource.pojo.GunIndexPOJO;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.sound.SoundManager;
import com.tacz.guns.util.ColorHex;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.commands.arguments.ParticleArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

@OnlyIn(Dist.CLIENT)
public class ClientGunIndex {
    private String name;
    private String thirdPersonAnimation = "empty";
    private BedrockGunModel gunModel;
    private @Nullable Pair<BedrockGunModel, ResourceLocation> lodModel;
    private GunAnimationStateMachine animationStateMachine;
    private @Nullable ResourceLocation playerAnimator3rd = new ResourceLocation(GunMod.MOD_ID, "rifle_default.player_animation");
    private Map<String, ResourceLocation> sounds;
    private GunTransform transform;
    private GunData gunData;
    private ResourceLocation modelTexture;
    private ResourceLocation slotTexture;
    private ResourceLocation hudTexture;
    private @Nullable ResourceLocation hudEmptyTexture;
    private String type;
    private String itemType;
    private @Nullable ShellEjection shellEjection;
    private @Nullable MuzzleFlash muzzleFlash;
    private LayerGunShow offhandShow;
    private @Nullable Int2ObjectArrayMap<LayerGunShow> hotbarShow;
    private float ironZoom;
    private boolean showCrosshair = false;
    private @Nullable AmmoParticle particle;
    private float @Nullable [] tracerColor = null;
    private EnumMap<FireMode, ControllableData> controllableData;
    private AmmoCountStyle ammoCountStyle = AmmoCountStyle.NORMAL;

    private ClientGunIndex() {
    }

    public static ClientGunIndex getInstance(GunIndexPOJO gunIndexPOJO) throws IllegalArgumentException {
        ClientGunIndex index = new ClientGunIndex();
        checkIndex(gunIndexPOJO, index);
        GunDisplay display = checkDisplay(gunIndexPOJO);
        checkData(gunIndexPOJO, index);
        checkName(gunIndexPOJO, index);
        checkTextureAndModel(display, index);
        checkLod(display, index);
        checkSlotTexture(display, index);
        checkHUDTexture(display, index);
        checkAnimation(display, index);
        checkSounds(display, index);
        checkTransform(display, index);
        checkShellEjection(display, index);
        checkGunAmmo(display, index);
        checkMuzzleFlash(display, index);
        checkLayerGunShow(display, index);
        checkIronZoom(display, index);
        checkTextShow(display, index);
        index.showCrosshair = display.isShowCrosshair();
        index.controllableData = display.getControllableData();
        index.ammoCountStyle = display.getAmmoCountStyle();
        return index;
    }

    private static void checkIndex(GunIndexPOJO gunIndexPOJO, ClientGunIndex index) {
        Preconditions.checkArgument(gunIndexPOJO != null, "index object file is empty");
        Preconditions.checkArgument(StringUtils.isNoneBlank(gunIndexPOJO.getType()), "index object missing type field");
        index.type = gunIndexPOJO.getType();
        index.itemType = gunIndexPOJO.getItemType();
    }

    private static void checkName(GunIndexPOJO gunIndexPOJO, ClientGunIndex index) {
        index.name = gunIndexPOJO.getName();
        if (StringUtils.isBlank(index.name)) {
            index.name = "custom.tacz.error.no_name";
        }
    }

    private static void checkData(GunIndexPOJO gunIndexPOJO, ClientGunIndex index) {
        ResourceLocation pojoData = gunIndexPOJO.getData();
        Preconditions.checkArgument(pojoData != null, "index object missing pojoData field");
        GunData data = CommonAssetManager.INSTANCE.getGunData(pojoData);
        Preconditions.checkArgument(data != null, "there is no corresponding data file");
        // 剩下的不需要校验了，Common的读取逻辑中已经校验过了
        index.gunData = data;
    }

    @NotNull
    private static GunDisplay checkDisplay(GunIndexPOJO gunIndexPOJO) {
        ResourceLocation pojoDisplay = gunIndexPOJO.getDisplay();
        Preconditions.checkArgument(pojoDisplay != null, "index object missing display field");
        GunDisplay display = ClientAssetManager.INSTANCE.getGunDisplay(pojoDisplay);
        Preconditions.checkArgument(display != null, "there is no corresponding display file");
        return display;
    }

    private static void checkIronZoom(GunDisplay display, ClientGunIndex index) {
        index.ironZoom = display.getIronZoom();
        if (index.ironZoom < 1) {
            index.ironZoom = 1;
        }
    }

    private static void checkTextShow(GunDisplay display, ClientGunIndex index) {
        Map<String, TextShow> textShowMap = Maps.newHashMap();
        display.getTextShows().forEach((key, value) -> {
            if (StringUtils.isNoneBlank(key)) {
                int color = ColorHex.colorTextToRbgInt(value.getColorText());
                value.setColorInt(color);
                textShowMap.put(key, value);
            }
        });
        index.gunModel.setTextShowList(textShowMap);
    }

    private static void checkTextureAndModel(GunDisplay display, ClientGunIndex index) {
        // 检查模型
        ResourceLocation modelLocation = display.getModelLocation();
        Preconditions.checkArgument(modelLocation != null, "display object missing model field");
        BedrockModelPOJO modelPOJO = ClientAssetManager.INSTANCE.getModels(modelLocation);
        Preconditions.checkArgument(modelPOJO != null, "there is no corresponding model file");
        // 检查默认材质是否存在
        ResourceLocation textureLocation = display.getModelTexture();
        Preconditions.checkArgument(textureLocation != null, "missing default texture");
        index.modelTexture = textureLocation;
        // 先判断是不是 1.10.0 版本基岩版模型文件
        if (BedrockVersion.isLegacyVersion(modelPOJO) && modelPOJO.getGeometryModelLegacy() != null) {
            index.gunModel = new BedrockGunModel(modelPOJO, BedrockVersion.LEGACY);
        }
        // 判定是不是 1.12.0 版本基岩版模型文件
        if (BedrockVersion.isNewVersion(modelPOJO) && modelPOJO.getGeometryModelNew() != null) {
            index.gunModel = new BedrockGunModel(modelPOJO, BedrockVersion.NEW);
        }
        Preconditions.checkArgument(index.gunModel != null, "there is no model data in the model file");
    }

    private static void checkLod(GunDisplay display, ClientGunIndex index) {
        GunLod gunLod = display.getGunLod();
        if (gunLod != null) {
            ResourceLocation texture = gunLod.getModelTexture();
            if (gunLod.getModelLocation() == null) {
                return;
            }
            if (texture == null) {
                return;
            }
            BedrockModelPOJO modelPOJO = ClientAssetManager.INSTANCE.getModels(gunLod.getModelLocation());
            if (modelPOJO == null) {
                return;
            }
            // 先判断是不是 1.10.0 版本基岩版模型文件
            if (BedrockVersion.isLegacyVersion(modelPOJO) && modelPOJO.getGeometryModelLegacy() != null) {
                BedrockGunModel model = new BedrockGunModel(modelPOJO, BedrockVersion.LEGACY);
                index.lodModel = Pair.of(model, texture);
            }
            // 判定是不是 1.12.0 版本基岩版模型文件
            if (BedrockVersion.isNewVersion(modelPOJO) && modelPOJO.getGeometryModelNew() != null) {
                BedrockGunModel model = new BedrockGunModel(modelPOJO, BedrockVersion.NEW);
                index.lodModel = Pair.of(model, texture);
            }
        }
    }

    private static void checkAnimation(GunDisplay display, ClientGunIndex index) {
        ResourceLocation location = display.getAnimationLocation();
        AnimationController controller;
        if (location == null) {
            controller = new AnimationController(Lists.newArrayList(), index.gunModel);
        } else {
            AnimationStructure gltfAnimations = ClientAssetManager.INSTANCE.getGltfAnimations(location);
            BedrockAnimationFile bedrockAnimationFile = ClientAssetManager.INSTANCE.getBedrockAnimations(location);
            if (bedrockAnimationFile != null) {
                // 用 bedrock 动画资源创建动画控制器
                controller = Animations.createControllerFromBedrock(bedrockAnimationFile, index.gunModel);
            } else if (gltfAnimations != null) {
                // 用 gltf 动画资源创建动画控制器
                controller = Animations.createControllerFromGltf(gltfAnimations, index.gunModel);
            } else {
                throw new IllegalArgumentException("animation not found: " + location);
            }
            // 将默认动画填入动画控制器
            DefaultAnimation defaultAnimation = display.getDefaultAnimation();
            if (defaultAnimation != null) {
                switch (defaultAnimation) {
                    case RIFLE -> {
                        for (ObjectAnimation animation : InternalAssetLoader.getDefaultRifleAnimations()) {
                            controller.providePrototypeIfAbsent(animation.name, () -> new ObjectAnimation(animation));
                        }
                    }
                    case PISTOL -> {
                        for (ObjectAnimation animation : InternalAssetLoader.getDefaultPistolAnimations()) {
                            controller.providePrototypeIfAbsent(animation.name, () -> new ObjectAnimation(animation));
                        }
                    }
                }
            }
        }
        // 将动画控制器包装起来
        index.animationStateMachine = new GunAnimationStateMachine(controller);
        // 初始化第三人称动画
        if (StringUtils.isNoneBlank(display.getThirdPersonAnimation())) {
            index.thirdPersonAnimation = display.getThirdPersonAnimation();
        }
        // player animator 兼容动画
        if (display.getPlayerAnimator3rd() != null) {
            index.playerAnimator3rd = display.getPlayerAnimator3rd();
        }
    }

    private static void checkSounds(GunDisplay display, ClientGunIndex index) {
        index.sounds = Maps.newHashMap();
        Map<String, ResourceLocation> soundMaps = display.getSounds();
        if (soundMaps == null || soundMaps.isEmpty()) {
            return;
        }
        // 部分音效为默认音效，不存在则需要添加默认音效
        soundMaps.putIfAbsent(SoundManager.DRY_FIRE_SOUND, new ResourceLocation(GunMod.MOD_ID, SoundManager.DRY_FIRE_SOUND));
        soundMaps.putIfAbsent(SoundManager.FIRE_SELECT, new ResourceLocation(GunMod.MOD_ID, SoundManager.FIRE_SELECT));
        soundMaps.putIfAbsent(SoundManager.HEAD_HIT_SOUND, new ResourceLocation(GunMod.MOD_ID, SoundManager.HEAD_HIT_SOUND));
        soundMaps.putIfAbsent(SoundManager.FLESH_HIT_SOUND, new ResourceLocation(GunMod.MOD_ID, SoundManager.FLESH_HIT_SOUND));
        soundMaps.putIfAbsent(SoundManager.KILL_SOUND, new ResourceLocation(GunMod.MOD_ID, SoundManager.KILL_SOUND));
        soundMaps.putIfAbsent(SoundManager.MELEE_BAYONET, new ResourceLocation(GunMod.MOD_ID, "melee_bayonet/melee_bayonet_01"));
        soundMaps.putIfAbsent(SoundManager.MELEE_STOCK, new ResourceLocation(GunMod.MOD_ID, "melee_stock/melee_stock_01"));
        soundMaps.putIfAbsent(SoundManager.MELEE_PUSH, new ResourceLocation(GunMod.MOD_ID, "melee_stock/melee_stock_02"));
        index.sounds.putAll(soundMaps);
    }

    private static void checkTransform(GunDisplay display, ClientGunIndex index) {
        GunTransform readTransform = display.getTransform();
        if (readTransform == null || readTransform.getScale() == null) {
            index.transform = GunTransform.getDefault();
        } else {
            index.transform = display.getTransform();
        }
    }

    private static void checkSlotTexture(GunDisplay display, ClientGunIndex index) {
        // 加载 GUI 内枪械图标
        index.slotTexture = Objects.requireNonNullElseGet(display.getSlotTextureLocation(), MissingTextureAtlasSprite::getLocation);
    }

    private static void checkHUDTexture(GunDisplay display, ClientGunIndex index) {
        index.hudTexture = Objects.requireNonNullElseGet(display.getHudTextureLocation(), MissingTextureAtlasSprite::getLocation);
        index.hudEmptyTexture = display.getHudEmptyTextureLocation();
    }

    private static void checkShellEjection(GunDisplay display, ClientGunIndex index) {
        index.shellEjection = display.getShellEjection();
    }

    private static void checkGunAmmo(GunDisplay display, ClientGunIndex index) {
        GunAmmo displayGunAmmo = display.getGunAmmo();
        if (displayGunAmmo == null) {
            return;
        }
        String tracerColorText = displayGunAmmo.getTracerColor();
        if (StringUtils.isNoneBlank(tracerColorText)) {
            index.tracerColor = ColorHex.colorTextToRbgFloatArray(tracerColorText);
        }
        AmmoParticle particle = displayGunAmmo.getParticle();
        if (particle != null) {
            try {
                String name = particle.getName();
                if (StringUtils.isNoneBlank()) {
                    particle.setParticleOptions(ParticleArgument.readParticle(new StringReader(name), BuiltInRegistries.PARTICLE_TYPE.asLookup()));
                    Preconditions.checkArgument(particle.getCount() > 0, "particle count must be greater than 0");
                    Preconditions.checkArgument(particle.getLifeTime() > 0, "particle life time must be greater than 0");
                    index.particle = particle;
                }
            } catch (CommandSyntaxException e) {
                e.fillInStackTrace();
            }
        }
    }

    private static void checkMuzzleFlash(GunDisplay display, ClientGunIndex index) {
        index.muzzleFlash = display.getMuzzleFlash();
        if (index.muzzleFlash != null && index.muzzleFlash.getTexture() == null) {
            index.muzzleFlash = null;
        }
    }

    private static void checkLayerGunShow(GunDisplay display, ClientGunIndex index) {
        index.offhandShow = display.getOffhandShow();
        if (index.offhandShow == null) {
            index.offhandShow = new LayerGunShow();
        }
        Map<String, LayerGunShow> show = display.getHotbarShow();
        if (show == null || show.isEmpty()) {
            return;
        }
        index.hotbarShow = new Int2ObjectArrayMap<>();
        for (String key : show.keySet()) {
            try {
                index.hotbarShow.put(Integer.parseInt(key), show.get(key));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("index number is error: " + key);
            }
        }
    }

    public String getType() {
        return type;
    }

    public String getItemType() {
        return itemType;
    }

    public String getName() {
        return name;
    }

    public BedrockGunModel getGunModel() {
        return gunModel;
    }

    @Nullable
    public Pair<BedrockGunModel, ResourceLocation> getLodModel() {
        return lodModel;
    }

    public GunAnimationStateMachine getAnimationStateMachine() {
        return animationStateMachine;
    }

    @Nullable
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

    @Nullable
    public ResourceLocation getHudEmptyTexture() {
        return hudEmptyTexture;
    }

    public ResourceLocation getModelTexture() {
        return modelTexture;
    }

    public GunData getGunData() {
        return gunData;
    }

    public String getThirdPersonAnimation() {
        return thirdPersonAnimation;
    }

    @Nullable
    public ShellEjection getShellEjection() {
        return shellEjection;
    }

    @Nullable
    public float[] getTracerColor() {
        return tracerColor;
    }

    @Nullable
    public AmmoParticle getParticle() {
        return particle;
    }

    @Nullable
    public MuzzleFlash getMuzzleFlash() {
        return muzzleFlash;
    }

    public LayerGunShow getOffhandShow() {
        return offhandShow;
    }

    @Nullable
    public Int2ObjectArrayMap<LayerGunShow> getHotbarShow() {
        return hotbarShow;
    }

    public float getIronZoom() {
        return ironZoom;
    }

    public boolean isShowCrosshair() {
        return showCrosshair;
    }

    public @Nullable ResourceLocation getPlayerAnimator3rd() {
        return playerAnimator3rd;
    }

    public EnumMap<FireMode, ControllableData> getControllableData() {
        return controllableData;
    }

    public AmmoCountStyle getAmmoCountStyle() {
        return ammoCountStyle;
    }
}