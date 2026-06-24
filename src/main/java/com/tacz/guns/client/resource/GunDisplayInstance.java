package com.tacz.guns.client.resource;

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
import com.tacz.guns.api.client.animation.statemachine.LuaAnimationStateMachine;
import com.tacz.guns.api.client.animation.statemachine.LuaStateMachineFactory;
import com.tacz.guns.api.client.other.GunModelTypeManager;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.client.animation.statemachine.GunAnimationStateContext;
import com.tacz.guns.client.model.BedrockAmmoModel;
import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.resource.pojo.animation.bedrock.BedrockAnimationFile;
import com.tacz.guns.client.resource.pojo.display.LaserConfig;
import com.tacz.guns.client.resource.pojo.display.ammo.AmmoParticle;
import com.tacz.guns.client.resource.pojo.display.ammo.HitSurfaceParticle;
import com.tacz.guns.client.resource.pojo.display.gun.*;
import com.tacz.guns.client.resource.pojo.model.BedrockModelPOJO;
import com.tacz.guns.client.resource.pojo.model.BedrockVersion;
import com.tacz.guns.client.sound.GunSoundPreload;
import com.tacz.guns.config.client.ResourceConfig;
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
import org.jetbrains.annotations.Nullable;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.BiFunction;

/**
 * 经过处理和校验的枪械显示数据
 */
@OnlyIn(Dist.CLIENT)
public class GunDisplayInstance {
    private final ResourceLocation displayId;
    private final GunDisplay display;
    private final Object loadLock = new Object();

    // 加载标志位
    private volatile boolean modelLoaded = false;
    private volatile boolean lodLoaded = false;
    private volatile boolean animationLoaded = false;

    // 错误标志位
    private volatile boolean modelLoadFailed = false;
    private volatile boolean lodLoadFailed = false;
    private volatile boolean animationLoadFailed = false;

    // 异步加载任务
    private volatile CompletableFuture<Void> modelWarmUpTask = null;
    private volatile CompletableFuture<Void> lodWarmUpTask = null;
    private volatile CompletableFuture<Void> animationWarmUpTask = null;

    private String thirdPersonAnimation = "empty";
    private BedrockGunModel gunModel;
    private @Nullable Pair<BedrockGunModel, ResourceLocation> lodModel;

    private LuaAnimationStateMachine<GunAnimationStateContext> animationStateMachine;
    private @Nullable LuaTable stateMachineParam;

    private @Nullable ResourceLocation playerAnimator3rd = new ResourceLocation(GunMod.MOD_ID, "rifle_default.player_animation");
    private boolean is3rdFixedHand = false;
    private Map<String, ResourceLocation> sounds = Maps.newHashMap();
    private List<ResourceLocation> preloadSounds = Lists.newArrayList();
    private GunTransform transform = GunTransform.getDefault();

    private ResourceLocation modelTexture;
    private ResourceLocation slotTexture = MissingTextureAtlasSprite.getLocation();
    private ResourceLocation hudTexture = MissingTextureAtlasSprite.getLocation();
    private @Nullable ResourceLocation hudEmptyTexture;

    private @Nullable ShellEjection shellEjection;
    private @Nullable BedrockAmmoModel shellModel;
    private @Nullable ResourceLocation shellTextureLocation;
    private @Nullable MuzzleFlash muzzleFlash;
    private LayerGunShow offhandShow = new LayerGunShow();
    private @Nullable Int2ObjectArrayMap<LayerGunShow> hotbarShow;
    private float ironZoom;
    private float modelFov;
    private float zoomModelFov;
    private boolean showCrosshair = false;
    private @Nullable AmmoParticle particle;
    private @Nullable List<HitSurfaceParticle> hitSurfaceParticles;
    private @Nullable Boolean bulletHoleEnabled;
    private float @Nullable [] tracerColor = null;
    private EnumMap<FireMode, ControllableData> controllableData = new EnumMap<>(FireMode.class);
    private AmmoCountStyle ammoCountStyle = AmmoCountStyle.NORMAL;
    private DamageStyle damageStyle = DamageStyle.PER_PROJECTILE;
    private @Nullable LaserConfig laserConfig;
    private boolean enableTransparency;

    GunDisplayInstance(ResourceLocation displayId, GunDisplay display) {
        this.displayId = displayId;
        this.display = Objects.requireNonNull(display, "display");
        initBase(display);
        if (!ResourceConfig.ENABLE_LAZY_CLIENT_ASSET_LOAD.get()) {
            ensureAnimationLoaded();
        }
    }

    public static GunDisplayInstance create(ResourceLocation displayId, GunDisplay display)  throws IllegalArgumentException {
        return new GunDisplayInstance(displayId, display);
    }

    // 后台预热
    public void warmUpModel() {
        if (!ResourceConfig.ENABLE_LAZY_CLIENT_ASSET_LOAD.get()) {
            ensureModelLoaded();
            return;
        }
        if (modelLoaded || modelWarmUpTask != null) {
            return;
        }
        synchronized (loadLock) {
            if (modelLoaded || modelWarmUpTask != null) {
                return;
            }
            scheduleModelWarmUpLocked();
        }
    }

    public void warmUpLod() {
        if (!ResourceConfig.ENABLE_LAZY_CLIENT_ASSET_LOAD.get()) {
            ensureLodLoaded();
            return;
        }
        if (lodLoaded || lodWarmUpTask != null) {
            return;
        }
        synchronized (loadLock) {
            if (lodLoaded || lodWarmUpTask != null) {
                return;
            }
            lodWarmUpTask = CompletableFuture.runAsync(this::loadLodIfNecessary, ClientAssetLoadDispatcher.executor());
            lodWarmUpTask.whenComplete((unused, throwable) -> {
                if (throwable != null) {
                    handleLodLoadFailure(throwable);
                }
            });
        }
    }

    public void warmUpRuntime() {
        if (!ResourceConfig.ENABLE_LAZY_CLIENT_ASSET_LOAD.get()) {
            ensureAnimationLoaded();
            return;
        }
        if (animationLoaded || animationWarmUpTask != null) {
            return;
        }
        synchronized (loadLock) {
            if (animationLoaded || animationWarmUpTask != null) {
                return;
            }
            scheduleAnimationWarmUpLocked();
        }
    }

    // 确保所需资源完成，没完成阻塞一下
    private void ensureModelLoaded() {
        if (modelLoaded || modelLoadFailed) {
            return;
        }
        CompletableFuture<Void> task = modelWarmUpTask;
        if (task == null) {
            try {
                loadModelIfNecessary();
            } catch (Throwable throwable) {
                handleModelLoadFailure(throwable);
            }
            return;
        }
        try {
            task.join();
        } catch (CompletionException exception) {
            handleModelLoadFailure(exception.getCause() == null ? exception : exception.getCause());
        }
    }

    private void ensureLodLoaded() {
        if (lodLoaded || lodLoadFailed) {
            return;
        }
        CompletableFuture<Void> task = lodWarmUpTask;
        if (task == null) {
            try {
                loadLodIfNecessary();
            } catch (Throwable throwable) {
                handleLodLoadFailure(throwable);
            }
            return;
        }
        try {
            task.join();
        } catch (CompletionException exception) {
            handleLodLoadFailure(exception.getCause() == null ? exception : exception.getCause());
        }
    }

    private void ensureAnimationLoaded() {
        if (animationLoaded || animationLoadFailed) {
            return;
        }
        ensureModelLoaded();
        if (!modelLoaded) {
            animationLoadFailed = true;
            return;
        }
        CompletableFuture<Void> task = animationWarmUpTask;
        if (task == null) {
            try {
                loadAnimationAfterModelReady();
            } catch (Throwable throwable) {
                handleAnimationLoadFailure(throwable);
            }
            return;
        }
        try {
            task.join();
        } catch (CompletionException exception) {
            handleAnimationLoadFailure(exception.getCause() == null ? exception : exception.getCause());
        }
    }

    // 加载任务
    private void loadModelIfNecessary() {
        if (modelLoaded) {
            return;
        }
        synchronized (loadLock) {
            if (modelLoaded) {
                return;
            }
            checkTextureAndModel(display);
            checkShellModel(display);
            checkTextShow(display);
            modelLoaded = true;
        }
    }

    private void loadLodIfNecessary() {
        if (lodLoaded) {
            return;
        }
        synchronized (loadLock) {
            if (lodLoaded) {
                return;
            }
            checkLod(display);
            lodLoaded = true;
        }
    }

    private CompletableFuture<Void> scheduleModelWarmUpLocked() {
        if (modelLoaded) {
            return CompletableFuture.completedFuture(null);
        }
        if (modelWarmUpTask == null) {
            modelWarmUpTask = CompletableFuture.runAsync(this::loadModelIfNecessary, ClientAssetLoadDispatcher.executor());
            modelWarmUpTask.whenComplete((unused, throwable) -> {
                if (throwable != null) {
                    handleModelLoadFailure(throwable);
                }
            });
        }
        return modelWarmUpTask;
    }

    private CompletableFuture<Void> scheduleAnimationWarmUpLocked() {
        if (animationLoaded) {
            return CompletableFuture.completedFuture(null);
        }
        if (animationWarmUpTask == null) {
            // 动画任务尝试挂到模型加载后面
            CompletableFuture<Void> modelTask = scheduleModelWarmUpLocked();
            animationWarmUpTask = modelTask.thenRunAsync(this::loadAnimationAfterModelReady, ClientAssetLoadDispatcher.executor());
            animationWarmUpTask.whenComplete((unused, throwable) -> {
                if (throwable != null) {
                    handleAnimationLoadFailure(throwable);
                }
            });
        }
        return animationWarmUpTask;
    }

    private void loadAnimationAfterModelReady() {
        if (animationLoaded) {
            return;
        }
        synchronized (loadLock) {
            if (animationLoaded) {
                return;
            }
            if (!modelLoaded) {
                throw new IllegalStateException("Gun animation requires model to be loaded first");
            }
            checkAnimation(display);
            animationLoaded = true;
        }
    }

    private void handleModelLoadFailure(Throwable throwable) {
        boolean shouldLog = false;
        synchronized (loadLock) {
            if (!modelLoaded) {
                shouldLog = !modelLoadFailed;
                modelWarmUpTask = null;
                modelLoadFailed = true;
            }
        }
        if (shouldLog) {
            GunMod.LOGGER.warn("Failed to load gun model {}", displayId, throwable);
        }
    }

    private void handleLodLoadFailure(Throwable throwable) {
        boolean shouldLog = false;
        synchronized (loadLock) {
            if (!lodLoaded) {
                shouldLog = !lodLoadFailed;
                lodWarmUpTask = null;
                lodLoadFailed = true;
            }
        }
        if (shouldLog) {
            GunMod.LOGGER.warn("Failed to load gun lod {}", displayId, throwable);
        }
    }

    private void handleAnimationLoadFailure(Throwable throwable) {
        boolean shouldLog = false;
        synchronized (loadLock) {
            if (!animationLoaded) {
                shouldLog = !animationLoadFailed;
                animationWarmUpTask = null;
                animationLoadFailed = true;
            }
        }
        if (shouldLog) {
            GunMod.LOGGER.warn("Failed to load gun animation runtime {}", displayId, throwable);
        }
    }

    // 基础数据直接读
    private void initBase(GunDisplay display) {
        checkSlotTexture(display);
        checkHUDTexture(display);
        checkSounds(display);
        checkTransform(display);
        checkShellEjection(display);
        checkGunAmmo(display);
        checkMuzzleFlash(display);
        checkLayerGunShow(display);
        checkIronZoom(display);
        checkModelFov(display);
        checkZoomModelFov(display);
        thirdPersonAnimation = StringUtils.defaultIfBlank(display.getThirdPersonAnimation(), "empty");
        if (display.getPlayerAnimator3rd() != null) {
            playerAnimator3rd = display.getPlayerAnimator3rd();
            is3rdFixedHand = display.is3rdFixedHand();
        }
        showCrosshair = display.isShowCrosshair();
        controllableData = display.getControllableData();
        ammoCountStyle = display.getAmmoCountStyle();
        damageStyle = display.getDamageStyle();
        laserConfig = display.getLaserConfig();
        enableTransparency = display.enablesTransparency();
    }

    private void checkIronZoom(GunDisplay display) {
        ironZoom = display.getIronZoom();
        if (ironZoom < 1) {
            ironZoom = 1;
        }
    }

    private void checkModelFov(GunDisplay display) {
        modelFov = display.getModelFov();
        if (modelFov <= 0) {
            modelFov = 70;
        }
    }

    private void checkZoomModelFov(GunDisplay display) {
        zoomModelFov = display.getZoomModelFov();
        if (zoomModelFov <= 0) {
            zoomModelFov = 70;
        }
    }

    private void checkTextShow(GunDisplay display) {
        Map<String, TextShow> textShowMap = Maps.newHashMap();
        display.getTextShows().forEach((key, value) -> {
            if (StringUtils.isNoneBlank(key)) {
                int color = ColorHex.colorTextToRbgInt(value.getColorText());
                value.setColorInt(color);
                textShowMap.put(key, value);
            }
        });
        gunModel.setTextShowList(textShowMap);
    }

    private void checkTextureAndModel(GunDisplay display) {
        //获取模型类型
        String modelType = display.getModelType();
        BiFunction<BedrockModelPOJO, BedrockVersion, ? extends BedrockGunModel> constructor = GunModelTypeManager.getModelInstanceConstructor(modelType);
        // 检查模型
        ResourceLocation modelLocation = display.getModelLocation();
        Preconditions.checkArgument(modelLocation != null, "display object missing model field");
        BedrockModelPOJO modelPOJO = ClientAssetsManager.INSTANCE.getBedrockModelPOJO(modelLocation);
        Preconditions.checkArgument(modelPOJO != null, "there is no corresponding model file");
        // 检查默认材质是否存在
        ResourceLocation textureLocation = display.getModelTexture();
        Preconditions.checkArgument(textureLocation != null, "missing default texture");
        modelTexture = textureLocation;
        // 先判断是不是 1.10.0 版本基岩版模型文件
        if (BedrockVersion.isLegacyVersion(modelPOJO) && modelPOJO.getGeometryModelLegacy() != null) {
            gunModel = constructor.apply(modelPOJO, BedrockVersion.LEGACY);
        }
        // 判定是不是 1.12.0 版本基岩版模型文件
        if (BedrockVersion.isNewVersion(modelPOJO) && modelPOJO.getGeometryModelNew() != null) {
            gunModel = constructor.apply(modelPOJO, BedrockVersion.NEW);
        }
        Preconditions.checkArgument(gunModel != null, "there is no model data in the model file");
    }

    private void checkLod(GunDisplay display) {
        GunLod gunLod = display.getGunLod();
        if (gunLod != null) {
            ResourceLocation texture = gunLod.getModelTexture();
            if (gunLod.getModelLocation() == null) {
                return;
            }
            if (texture == null) {
                return;
            }
            BedrockModelPOJO modelPOJO = ClientAssetsManager.INSTANCE.getBedrockModelPOJO(gunLod.getModelLocation());
            if (modelPOJO == null) {
                return;
            }
            // 先判断是不是 1.10.0 版本基岩版模型文件
            if (BedrockVersion.isLegacyVersion(modelPOJO) && modelPOJO.getGeometryModelLegacy() != null) {
                BedrockGunModel model = new BedrockGunModel(modelPOJO, BedrockVersion.LEGACY);
                lodModel = Pair.of(model, texture);
            }
            // 判定是不是 1.12.0 版本基岩版模型文件
            if (BedrockVersion.isNewVersion(modelPOJO) && modelPOJO.getGeometryModelNew() != null) {
                BedrockGunModel model = new BedrockGunModel(modelPOJO, BedrockVersion.NEW);
                lodModel = Pair.of(model, texture);
            }
        }
    }

    private void checkAnimation(GunDisplay display) {
        ResourceLocation location = display.getAnimationLocation();
        AnimationController controller;
        if (location == null) {
            controller = new AnimationController(Lists.newArrayList(), gunModel);
        } else {
            AnimationStructure gltfAnimations = ClientAssetsManager.INSTANCE.getGltfAnimation(location);
            BedrockAnimationFile bedrockAnimationFile = ClientAssetsManager.INSTANCE.getBedrockAnimations(location);
            if (bedrockAnimationFile != null) {
                // 用 bedrock 动画资源创建动画控制器
                controller = Animations.createControllerFromBedrock(bedrockAnimationFile, gunModel);
            } else if (gltfAnimations != null) {
                // 用 gltf 动画资源创建动画控制器
                controller = Animations.createControllerFromGltf(gltfAnimations, gunModel);
            } else {
                throw new IllegalArgumentException("animation not found: " + location);
            }
            // 将默认动画填入动画控制器
            ResourceLocation defaultAnimation = display.getDefaultAnimation();
            if (defaultAnimation != null) {
                BedrockAnimationFile animationFile = ClientAssetsManager.INSTANCE.getBedrockAnimations(defaultAnimation);
                if (animationFile == null) {
                    throw new IllegalArgumentException("animation not found: " + defaultAnimation);
                }
                List<ObjectAnimation> animations = Animations.createAnimationFromBedrock(animationFile);
                for (ObjectAnimation animation : animations) {
                    controller.providePrototypeIfAbsent(animation.name, () -> new ObjectAnimation(animation));
                }
            } else {
                DefaultAnimationType defaultAnimationType = display.getDefaultAnimationType();
                if (defaultAnimationType != null) {
                    switch (defaultAnimationType) {
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
        }
        // 初始化动画状态机，将动画控制器封装进去。
        ResourceLocation stateMachineLocation = display.getStateMachineLocation();
        if (stateMachineLocation == null) {
            // 如果没指定状态机，则使用默认状态机
            stateMachineLocation = new ResourceLocation("tacz", "default_state_machine");
        }
        LuaTable script = ClientAssetsManager.INSTANCE.getScript(stateMachineLocation);
        if (script != null) {
            animationStateMachine = new LuaStateMachineFactory<GunAnimationStateContext>()
                    .setController(controller)
                    .setLuaScripts(script)
                    .build();
        } else {
            throw new IllegalArgumentException("statemachine not found: " + stateMachineLocation);
        }
        // 加载状态机参数
        Map<String, Object> params = display.getStateMachineParam();
        if (params != null) {
            stateMachineParam = new LuaTable();
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                stateMachineParam.set(entry.getKey(), CoerceJavaToLua.coerce(entry.getValue()));
            }
        }
    }

    private void checkSounds(GunDisplay display) {
        sounds = Maps.newHashMap();
        preloadSounds = Lists.newArrayList();
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
        sounds.putAll(soundMaps);

        Set<ResourceLocation> preloadSet = new LinkedHashSet<>();
        for (String name : GunSoundPreload.DEFAULT_PRELOAD_NAMES) {
            addPreloadSound(preloadSet, name);
        }
        List<String> configuredPreloadSounds = display.getPreloadSounds();
        if (configuredPreloadSounds != null) {
            for (String name : configuredPreloadSounds) {
                addPreloadSound(preloadSet, name);
            }
        }
        preloadSounds.addAll(preloadSet);
    }

    private void addPreloadSound(Set<ResourceLocation> preloadSet, String name) {
        if (StringUtils.isBlank(name)) {
            return;
        }
        ResourceLocation soundId = sounds.get(name);
        if (soundId != null) {
            preloadSet.add(soundId);
        }
    }

    private void checkTransform(GunDisplay display) {
        GunTransform readTransform = display.getTransform();
        if (readTransform == null || readTransform.getScale() == null) {
            transform = GunTransform.getDefault();
        } else {
            transform = display.getTransform();
        }
    }

    private void checkSlotTexture(GunDisplay display) {
        // 加载 GUI 内枪械图标
        slotTexture = Objects.requireNonNullElseGet(display.getSlotTextureLocation(), MissingTextureAtlasSprite::getLocation);
    }

    private void checkHUDTexture(GunDisplay display) {
        hudTexture = Objects.requireNonNullElseGet(display.getHudTextureLocation(), MissingTextureAtlasSprite::getLocation);
        hudEmptyTexture = display.getHudEmptyTextureLocation();
    }

    private void checkShellEjection(GunDisplay display) {
        shellEjection = display.getShellEjection();
    }

    private void checkShellModel(GunDisplay display) {
        ShellEjection shell = display.getShellEjection();
        if (shell == null || shell.getModelLocation() == null || shell.getModelTexture() == null) {
            return;
        }
        shellTextureLocation = shell.getModelTexture();
        BedrockModelPOJO modelPOJO = ClientAssetsManager.INSTANCE.getBedrockModelPOJO(shell.getModelLocation());
        if (modelPOJO == null) {
            return;
        }
        if (BedrockVersion.isLegacyVersion(modelPOJO) && modelPOJO.getGeometryModelLegacy() != null) {
            shellModel = new BedrockAmmoModel(modelPOJO, BedrockVersion.LEGACY);
        }
        if (BedrockVersion.isNewVersion(modelPOJO) && modelPOJO.getGeometryModelNew() != null) {
            shellModel = new BedrockAmmoModel(modelPOJO, BedrockVersion.NEW);
        }
    }

    private void checkGunAmmo(GunDisplay display) {
        GunAmmo displayGunAmmo = display.getGunAmmo();
        if (displayGunAmmo == null) {
            return;
        }
        String tracerColorText = displayGunAmmo.getTracerColor();
        if (StringUtils.isNoneBlank(tracerColorText)) {
            tracerColor = ColorHex.colorTextToRbgFloatArray(tracerColorText);
        }
        AmmoParticle particle = displayGunAmmo.getParticle();
        if (particle != null) {
            try {
                String name = particle.getName();
                if (StringUtils.isNoneBlank()) {
                    particle.setParticleOptions(ParticleArgument.readParticle(new StringReader(name), BuiltInRegistries.PARTICLE_TYPE.asLookup()));
                    Preconditions.checkArgument(particle.getCount() > 0, "particle count must be greater than 0");
                    Preconditions.checkArgument(particle.getLifeTime() > 0, "particle life time must be greater than 0");
                    this.particle = particle;
                }
            } catch (CommandSyntaxException e) {
                e.fillInStackTrace();
            }
        }
        hitSurfaceParticles = displayGunAmmo.getHitSurfaceParticles();
        bulletHoleEnabled = displayGunAmmo.getBulletHoleEnabled();
    }

    private void checkMuzzleFlash(GunDisplay display) {
        muzzleFlash = display.getMuzzleFlash();
        if (muzzleFlash != null && muzzleFlash.getTexture() == null) {
            muzzleFlash = null;
        }
    }

    private void checkLayerGunShow(GunDisplay display) {
        offhandShow = display.getOffhandShow();
        if (offhandShow == null) {
            offhandShow = new LayerGunShow();
        }
        Map<String, LayerGunShow> show = display.getHotbarShow();
        if (show == null || show.isEmpty()) {
            return;
        }
        hotbarShow = new Int2ObjectArrayMap<>();
        for (String key : show.keySet()) {
            try {
                hotbarShow.put(Integer.parseInt(key), show.get(key));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("index number is error: " + key);
            }
        }
    }

    public @Nullable BedrockGunModel getGunModel() {
        ensureModelLoaded();
        return gunModel;
    }

    @Nullable
    public Pair<BedrockGunModel, ResourceLocation> getLodModel() {
        ensureLodLoaded();
        return lodModel;
    }

    public @Nullable LuaAnimationStateMachine<GunAnimationStateContext> getAnimationStateMachine() {
        ensureAnimationLoaded();
        return animationStateMachine;
    }

    public @Nullable LuaTable getStateMachineParam() {
        ensureAnimationLoaded();
        return stateMachineParam;
    }

    @Nullable
    public ResourceLocation getSounds(String name) {
        return sounds.get(name);
    }

    public List<ResourceLocation> getPreloadSounds() {
        return preloadSounds;
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
        ensureModelLoaded();
        return modelTexture != null ? modelTexture : MissingTextureAtlasSprite.getLocation();
    }

    public String getThirdPersonAnimation() {
        return thirdPersonAnimation;
    }

    @Nullable
    public ShellEjection getShellEjection() {
        return shellEjection;
    }

    @Nullable
    public BedrockAmmoModel getShellModel() {
        ensureModelLoaded();
        return shellModel;
    }

    @Nullable
    public ResourceLocation getShellTextureLocation() {
        ensureModelLoaded();
        return shellTextureLocation;
    }

    public float @Nullable [] getTracerColor() {
        return tracerColor;
    }

    @Nullable
    public AmmoParticle getParticle() {
        return particle;
    }

    @Nullable
    public List<HitSurfaceParticle> getHitSurfaceParticles() {
        return hitSurfaceParticles;
    }

    @Nullable
    public Boolean getBulletHoleEnabled() {
        return bulletHoleEnabled;
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

    public float getModelFov() {
        return modelFov;
    }

    public float getZoomModelFov() {
        return zoomModelFov;
    }

    public boolean isShowCrosshair() {
        return showCrosshair;
    }

    public @Nullable ResourceLocation getPlayerAnimator3rd() {
        return playerAnimator3rd;
    }

    public boolean is3rdFixedHand() {
        return is3rdFixedHand;
    }

    public EnumMap<FireMode, ControllableData> getControllableData() {
        return controllableData;
    }

    public AmmoCountStyle getAmmoCountStyle() {
        return ammoCountStyle;
    }

    public DamageStyle getDamageStyle() {
        return damageStyle;
    }

    public @Nullable LaserConfig getLaserConfig() {
        return laserConfig;
    }

    public boolean enablesTransparency() {
        return enableTransparency;
    }
}
