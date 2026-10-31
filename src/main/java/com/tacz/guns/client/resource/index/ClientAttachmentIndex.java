package com.tacz.guns.client.resource.index;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.tacz.guns.GunMod;
import com.tacz.guns.client.model.BedrockAttachmentModel;
import com.tacz.guns.client.model.DisplayGunModelInstance;
import com.tacz.guns.client.model.ExternalGunModel;
import com.tacz.guns.client.resource.ClientAssetLoadDispatcher;
import com.tacz.guns.client.resource.ClientAssetLoadTask;
import com.tacz.guns.client.resource.ClientAssetsManager;
import com.tacz.guns.client.resource.ExternalModelAssets;
import com.tacz.guns.client.resource.pojo.display.LaserConfig;
import com.tacz.guns.client.resource.pojo.display.SoundData;
import com.tacz.guns.client.resource.pojo.display.attachment.AttachmentDisplay;
import com.tacz.guns.client.resource.pojo.display.attachment.AttachmentLod;
import com.tacz.guns.client.resource.pojo.display.attachment.ScopeLensConfig;
import com.tacz.guns.client.resource.pojo.display.gun.TextShow;
import com.tacz.guns.client.resource.pojo.model.BedrockModelPOJO;
import com.tacz.guns.client.resource.pojo.model.BedrockVersion;
import com.tacz.guns.config.client.ResourceConfig;
import com.tacz.guns.resource.CommonAssetsManager;
import com.tacz.guns.resource.pojo.AttachmentIndexPOJO;
import com.tacz.guns.resource.pojo.data.attachment.AttachmentData;
import com.tacz.guns.util.ColorHex;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class ClientAttachmentIndex {
    private final Object modelLoadLock = new Object();
    private final Map<ResourceLocation, ClientAttachmentSkinIndex> skinIndexMap = Maps.newHashMap();
    private String name;
    private AttachmentDisplay display;
    private @Nullable BedrockAttachmentModel attachmentModel;
    private @Nullable ResourceLocation modelTexture;
    private @Nullable Pair<BedrockAttachmentModel, ResourceLocation> lodModel;

    private final ExternalModelAssets externalModels = new ExternalModelAssets();
    private final ClientAssetLoadTask<ExternalGunModel> externalModel = externalModels.add(
            () -> display.getModel(),
            this::bakeExternalModel,
            failure -> {
                GunMod.LOGGER.warn("Failed to bake SBM attachment model {}", display.getModel(), failure);
            }
    );

    private final ClientAssetLoadTask<Pair<ExternalGunModel, ResourceLocation>> externalLodModel =
            externalModels.add(
                    this::externalLodResource,
                    this::bakeExternalLodModel,
                    failure -> {
                        GunMod.LOGGER.warn("Failed to bake SBM attachment LOD {}", display.getModel(), failure);
                    }
            );

    private ResourceLocation slotTexture;
    private AttachmentData data;
    private float[] viewsFov;
    private float @Nullable [] zoom;
    private int [] views;
    private float idleScopeHeightAdjust;
    private boolean isScope;
    private boolean isSight;
    private boolean showMuzzle;
    private boolean showMount;
    private float muzzleFlashOffset;
    private boolean hasRangefinder;
    private boolean hasImpactPrediction;
    private @Nullable String thermalImagingTooltipKey;
    private boolean hasAmmoCounter;
    private @Nullable String adapterNodeName;
    private @Nullable String tooltipKey;
    private Map<String, SoundData> sounds;
    private @Nullable LaserConfig laserConfig;
    private volatile boolean modelsLoaded = false;
    private volatile boolean modelsLoadFailed = false;
    private volatile CompletableFuture<Void> warmUpTask = null;

    private ClientAttachmentIndex() {
    }

    public static ClientAttachmentIndex getInstance(ResourceLocation registryName, AttachmentIndexPOJO indexPOJO) throws IllegalArgumentException {
        ClientAttachmentIndex index = new ClientAttachmentIndex();
        checkIndex(indexPOJO, index);
        AttachmentDisplay display = checkDisplay(indexPOJO, index);
        checkData(indexPOJO, index);
        checkName(indexPOJO, index);
        checkSlotTexture(display, index);
        // 配件皮肤加载暂未启用：checkSkins(registryName, index);
        checkSounds(display, index);
        if (!ResourceConfig.ENABLE_LAZY_CLIENT_ASSET_LOAD.get()) {
            index.ensureModelsLoaded();
        }
        return index;
    }

    private static void checkIndex(AttachmentIndexPOJO attachmentIndexPOJO, ClientAttachmentIndex index) {
        Preconditions.checkArgument(attachmentIndexPOJO != null, "index object file is empty");
        index.tooltipKey = attachmentIndexPOJO.getTooltip();
    }

    @Nonnull
    private static AttachmentDisplay checkDisplay(AttachmentIndexPOJO indexPOJO, ClientAttachmentIndex index) {
        ResourceLocation pojoDisplay = indexPOJO.getDisplay();
        Preconditions.checkArgument(pojoDisplay != null, "index object missing display field");
        AttachmentDisplay display = ClientAssetsManager.INSTANCE.getAttachmentDisplay(pojoDisplay);
        Preconditions.checkArgument(display != null, "there is no corresponding display file");
        index.display = display;
        index.viewsFov = display.getViewsFov();
        if (index.viewsFov == null) {
            Preconditions.checkArgument(display.getFov() > 0, "fov must > 0");
            index.viewsFov = new float[]{display.getFov()};
        } else {
            for(float fov : index.viewsFov) {
                Preconditions.checkArgument(fov > 0, "fov must > 0");
            }
        }
        index.zoom = display.getZoom();
        if (index.zoom != null) {
            for (int i = 0; i < index.zoom.length; i++) {
                if (index.zoom[i] < 1) {
                    throw new IllegalArgumentException("zoom must >= 1");
                }
            }
        }
        index.views = display.getViews();
        if (index.views == null) {
            index.views = new int[]{1};
        } else {
            for (int i = 0; i < index.views.length; i++) {
                if (index.views[i] < 1) {
                    throw new IllegalArgumentException("view index must >= 1");
                }
            }
        }
        index.isScope = display.isScope();
        index.isSight = display.isSight();
        index.idleScopeHeightAdjust = display.getIdleScopeHeightAdjust();
        index.adapterNodeName = display.getAdapterNodeName();
        index.showMuzzle = display.isShowMuzzle();
        index.showMount = display.isShowMount();
        index.muzzleFlashOffset = display.getMuzzleFlashOffset();
        index.hasRangefinder = display.getTextShows().values().stream().anyMatch(textShow -> textShow.getRangefinder() != null);
        index.hasImpactPrediction = display.getTextShows().values().stream()
                .anyMatch(textShow -> textShow.getRangefinder() != null && textShow.getRangefinder().isShowImpactPrediction());
        index.thermalImagingTooltipKey = switch (display.getScopeLens().getVisionMode()) {
            case THERMAL_WHITE_HOT -> "tooltip.tacz.attachment.thermal_imaging.white_hot";
            case THERMAL_BLACK_HOT -> "tooltip.tacz.attachment.thermal_imaging.black_hot";
            case THERMAL_IRONBOW -> "tooltip.tacz.attachment.thermal_imaging.ironbow";
            default -> null;
        };
        index.hasAmmoCounter = display.getTextShows().values().stream()
                .anyMatch(textShow -> textShow.getTextKey() != null
                        && I18n.language.getOrDefault(textShow.getTextKey()).contains("%ammo_count%"));
        index.laserConfig = display.getLaserConfig();
        return display;
    }

    private static void checkTextShow(AttachmentDisplay display, BedrockAttachmentModel model) {
        if (model != null) {
            Map<String, TextShow> textShowMap = Maps.newHashMap();
            display.getTextShows().forEach((key, textShow) -> {
                if (StringUtils.isNoneBlank(key)) {
                    int color = ColorHex.colorTextToRbgInt(textShow.getColorText());
                    textShow.setColorInt(color);
                    textShowMap.put(key, textShow);
                }
            });
            model.setTextShowList(textShowMap);
        }
    }

    private static void checkData(AttachmentIndexPOJO indexPOJO, ClientAttachmentIndex index) {
        ResourceLocation dataId = indexPOJO.getData();
        Preconditions.checkArgument(dataId != null, "index object missing pojoData field");
        AttachmentData data = CommonAssetsManager.get().getAttachmentData(dataId);
        Preconditions.checkArgument(data != null, "there is no corresponding data file");
        // 剩下的不需要校验了，Common的读取逻辑中已经校验过了
        index.data = data;
    }

    private static void checkName(AttachmentIndexPOJO indexPOJO, ClientAttachmentIndex index) {
        index.name = indexPOJO.getName();
        if (StringUtils.isBlank(index.name)) {
            index.name = "custom.tacz.error.no_name";
        }
    }

    private static void checkSlotTexture(AttachmentDisplay display, ClientAttachmentIndex index) {
        index.slotTexture = Objects.requireNonNullElseGet(display.getSlotTextureLocation(), MissingTextureAtlasSprite::getLocation);
    }

    private static void checkTextureAndModel(AttachmentDisplay display, ClientAttachmentIndex index) {
        // 不检查模型/材质是否为 null，模型/材质可以为 null
        index.attachmentModel = getOrLoadAttachmentModel(display.getModel());
        if (index.attachmentModel != null) {
            index.attachmentModel.setIsScope(display.isScope());
            index.attachmentModel.setIsSight(display.isSight());
            index.attachmentModel.setScopeLens(display.getScopeLens());
        }
        index.modelTexture = display.getTexture();
    }

    public void warmUp() {
        warmUpLegacyModels();
        externalModel.warmUp();
        externalLodModel.warmUp();
    }

    private void warmUpLegacyModels() {
        if (!ResourceConfig.ENABLE_LAZY_CLIENT_ASSET_LOAD.get()) {
            ensureModelsLoaded();
            return;
        }
        if (modelsLoaded || modelsLoadFailed || warmUpTask != null) {
            return;
        }
        synchronized (modelLoadLock) {
            if (modelsLoaded || modelsLoadFailed || warmUpTask != null) {
                return;
            }
            warmUpTask = CompletableFuture.runAsync(this::loadModelsIfNecessary, ClientAssetLoadDispatcher.executor());
            warmUpTask.whenComplete((unused, throwable) -> {
                if (throwable != null) {
                    handleModelsLoadFailure(throwable);
                }
            });
        }
    }

    private void ensureModelsLoaded() {
        if (modelsLoaded || modelsLoadFailed) {
            return;
        }
        CompletableFuture<Void> task = warmUpTask;
        if (task == null) {
            try {
                loadModelsIfNecessary();
            } catch (Throwable throwable) {
                handleModelsLoadFailure(throwable);
            }
            return;
        }
        try {
            task.join();
        } catch (CompletionException exception) {
            handleModelsLoadFailure(exception.getCause() == null ? exception : exception.getCause());
        }
    }

    private void loadModelsIfNecessary() {
        if (modelsLoaded) {
            return;
        }
        synchronized (modelLoadLock) {
            if (modelsLoaded) {
                return;
            }
            checkTextureAndModel(display, this);
            checkTextShow(display, attachmentModel);
            checkLod(display, this);
            modelsLoaded = true;
        }
    }

    private void handleModelsLoadFailure(Throwable throwable) {
        boolean shouldLog = false;
        synchronized (modelLoadLock) {
            if (!modelsLoaded) {
                shouldLog = !modelsLoadFailed;
                warmUpTask = null;
                modelsLoadFailed = true;
            }
        }
        if (shouldLog) {
            GunMod.LOGGER.warn("Failed to load attachment models {}", display.getModel(), throwable);
        }
    }

    @Nullable
    public static BedrockAttachmentModel getOrLoadAttachmentModel(@Nullable ResourceLocation modelLocation) {
        if (modelLocation == null) {
            return null;
        }
        BedrockModelPOJO modelPOJO = ClientAssetsManager.INSTANCE.getBedrockModelPOJO(modelLocation);
        if (modelPOJO == null) {
            return null;
        }
        return getAttachmentModel(modelPOJO);
    }

    @Nullable
    public static BedrockAttachmentModel getAttachmentModel(BedrockModelPOJO modelPOJO) {
        BedrockAttachmentModel attachmentModel = null;
        // 先判断是不是 1.10.0 版本基岩版模型文件
        if (BedrockVersion.isLegacyVersion(modelPOJO) && modelPOJO.getGeometryModelLegacy() != null) {
            attachmentModel = new BedrockAttachmentModel(modelPOJO, BedrockVersion.LEGACY);
        }
        // 判定是不是 1.12.0 版本基岩版模型文件
        if (BedrockVersion.isNewVersion(modelPOJO) && modelPOJO.getGeometryModelNew() != null) {
            attachmentModel = new BedrockAttachmentModel(modelPOJO, BedrockVersion.NEW);
        }
        return attachmentModel;
    }

    private static void checkLod(AttachmentDisplay display, ClientAttachmentIndex index) {
        AttachmentLod gunLod = display.getAttachmentLod();
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
                BedrockAttachmentModel model = new BedrockAttachmentModel(modelPOJO, BedrockVersion.LEGACY);
                index.lodModel = Pair.of(model, texture);
            }
            // 判定是不是 1.12.0 版本基岩版模型文件
            if (BedrockVersion.isNewVersion(modelPOJO) && modelPOJO.getGeometryModelNew() != null) {
                BedrockAttachmentModel model = new BedrockAttachmentModel(modelPOJO, BedrockVersion.NEW);
                index.lodModel = Pair.of(model, texture);
            }
        }
    }


    private static void checkSounds(AttachmentDisplay display, ClientAttachmentIndex index) {
        Map<String, SoundData> displaySounds = display.getSounds();
        if (displaySounds == null) {
            index.sounds = Maps.newHashMap();
            return;
        }
        index.sounds = displaySounds;
    }

    public String getName() {
        return name;
    }

    @Nullable
    public String getTooltipKey() {
        return tooltipKey;
    }

    @Nullable
    public BedrockAttachmentModel getAttachmentModel() {
        ensureModelsLoaded();
        return attachmentModel;
    }

    @Nullable
    public ExternalGunModel getExternalModel() {
        return externalModel.getIfReady();
    }

    private @Nullable ExternalGunModel bakeExternalModel() {
        if (!loadModelsForExternalCapture() || attachmentModel == null || display.getModel() == null) {
            return null;
        }

        BedrockModelPOJO source = ClientAssetsManager.INSTANCE.getBedrockModelPOJO(display.getModel());
        if (source == null) {
            return null;
        }
        return ExternalGunModel.bake(
                source, getExternalHiddenTextBones(), DisplayGunModelInstance.Kind.ATTACHMENT
        );
    }

    private boolean loadModelsForExternalCapture() {
        if (modelsLoadFailed) {
            return false;
        }

        try {
            loadModelsIfNecessary();
        } catch (RuntimeException failure) {
            handleModelsLoadFailure(failure);
            return false;
        }
        return modelsLoaded;
    }

    public Set<String> getExternalHiddenTextBones() {
        return display.getTextShows() == null ? Set.of() : Set.copyOf(display.getTextShows().keySet());
    }

    @Nullable
    public Pair<ExternalGunModel, ResourceLocation> getExternalLodModel() {
        return externalLodModel.getIfReady();
    }

    private @Nullable Pair<ExternalGunModel, ResourceLocation> bakeExternalLodModel() {
        if (!loadModelsForExternalCapture() || lodModel == null) {
            return null;
        }

        AttachmentLod lod = display.getAttachmentLod();
        BedrockModelPOJO source = ClientAssetsManager.INSTANCE.getBedrockModelPOJO(lod.getModelLocation());
        if (source == null) {
            return null;
        }
        ExternalGunModel model = ExternalGunModel.bake(
                source, getExternalHiddenTextBones(), DisplayGunModelInstance.Kind.ATTACHMENT
        );
        return Pair.of(model, lod.getModelTexture());
    }

    public ExternalModelAssets getExternalModels() {
        return externalModels;
    }

    private ResourceLocation externalLodResource() {
        AttachmentLod lod = display.getAttachmentLod();
        if (lod != null && lod.getModelLocation() != null) {
            return lod.getModelLocation();
        }
        return display.getModel();
    }

    @Nullable
    public ResourceLocation getModelTexture() {
        ensureModelsLoaded();
        return modelTexture;
    }

    @Nullable
    public Pair<BedrockAttachmentModel, ResourceLocation> getLodModel() {
        ensureModelsLoaded();
        return lodModel;
    }

    public ResourceLocation getSlotTexture() {
        return slotTexture;
    }

    public float[] getViewsFov() {
        return viewsFov;
    }

    public float @Nullable [] getZoom() {
        return zoom;
    }

    public int[] getViews() {
        return views;
    }

    public float getIdleScopeHeightAdjust() {
        return idleScopeHeightAdjust;
    }

    public AttachmentData getData() {
        return data;
    }

    @Deprecated
    @Nullable
    public ClientAttachmentSkinIndex getSkinIndex(@Nullable ResourceLocation skinName) {
        return null;
    }

    public boolean isScope() {
        return isScope;
    }

    public ScopeLensConfig getScopeLens() {
        return display.getScopeLens();
    }

    public @Nullable String getThermalImagingTooltipKey() {
        return thermalImagingTooltipKey;
    }

    public boolean hasAmmoCounter() {
        return hasAmmoCounter;
    }

    public boolean isSight() {
        return isSight;
    }

    @Nullable
    public String getAdapterNodeName() {
        return adapterNodeName;
    }

    public boolean isShowMuzzle() {
        return showMuzzle;
    }

    public boolean isShowMount() {
        return showMount;
    }

    public float getMuzzleFlashOffset() {
        return muzzleFlashOffset;
    }

    public boolean hasRangefinder() {
        return hasRangefinder;
    }

    public boolean hasImpactPrediction() {
        return hasImpactPrediction;
    }

    public Map<String, SoundData> getSounds() {
        return sounds;
    }

    public @Nullable LaserConfig getLaserConfig() {
        return laserConfig;
    }
}
