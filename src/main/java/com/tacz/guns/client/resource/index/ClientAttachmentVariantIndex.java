package com.tacz.guns.client.resource.index;

import com.google.common.base.Preconditions;
import com.tacz.guns.GunMod;
import com.tacz.guns.client.model.BedrockAttachmentModel;
import com.tacz.guns.client.model.DisplayGunModelInstance;
import com.tacz.guns.client.model.ExternalGunModel;
import com.tacz.guns.client.resource.ClientAssetLoadDispatcher;
import com.tacz.guns.client.resource.ClientAssetLoadTask;
import com.tacz.guns.client.resource.ClientAssetsManager;
import com.tacz.guns.client.resource.ExternalModelAssets;
import com.tacz.guns.client.resource.pojo.display.attachment.AttachmentDisplay;
import com.tacz.guns.client.resource.pojo.model.BedrockModelPOJO;
import com.tacz.guns.resource.index.CommonAttachmentVariantIndex;
import com.tacz.guns.resource.pojo.AttachmentVariantIndexPOJO;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class ClientAttachmentVariantIndex {
    private final Object modelLoadLock = new Object();
    private Component name;
    private AttachmentDisplay display;
    private @Nullable BedrockAttachmentModel attachmentModel;
    private @Nullable ResourceLocation modelTexture;

    private final ExternalModelAssets externalModels = new ExternalModelAssets();
    private final ClientAssetLoadTask<ExternalGunModel> externalModel = externalModels.add(
            () -> display.getModel(),
            this::bakeExternalModel,
            failure -> {
                GunMod.LOGGER.warn("Failed to bake SBM attachment variant model {}", display.getModel(), failure);
            }
    );

    private float[] viewsFov;
    private float @Nullable [] zoom;
    private int[] views;
    private boolean scope;
    private boolean sight;
    private String mode;
    private @Nullable Vector3f mountOffset;
    private volatile boolean modelsLoaded = false;
    private volatile boolean modelsLoadFailed = false;
    private volatile CompletableFuture<Void> warmUpTask = null;

    private ClientAttachmentVariantIndex() {
    }

    public static ClientAttachmentVariantIndex getInstance(CommonAttachmentVariantIndex commonIndex) {
        ClientAttachmentVariantIndex index = new ClientAttachmentVariantIndex();
        AttachmentVariantIndexPOJO pojo = commonIndex.getPojo();
        checkIndex(pojo, index);
        checkDisplay(pojo, index);
        return index;
    }

    private static void checkIndex(AttachmentVariantIndexPOJO pojo, ClientAttachmentVariantIndex index) {
        Preconditions.checkArgument(pojo != null, "index object file is empty");
        index.name = pojo.getName() == null ? Component.empty() : Component.translatable(pojo.getName());
    }

    private static void checkDisplay(AttachmentVariantIndexPOJO pojo, ClientAttachmentVariantIndex index) {
        ResourceLocation displayId = pojo.getDisplay();
        Preconditions.checkArgument(displayId != null, "index object missing display field");
        AttachmentDisplay display = ClientAssetsManager.INSTANCE.getAttachmentVariantDisplay(displayId);
        Preconditions.checkArgument(display != null, "there is no corresponding display file");
        index.display = display;
        index.viewsFov = display.getViewsFov();
        if (index.viewsFov == null) {
            Preconditions.checkArgument(display.getFov() > 0, "fov must > 0");
            index.viewsFov = new float[]{display.getFov()};
        }
        index.zoom = display.getZoom();
        index.views = display.getViews() == null ? new int[]{1} : display.getViews();
        index.scope = display.isScope();
        index.sight = display.isSight();
        index.mode = display.getVariantMode();
        index.mountOffset = display.getMountOffset();
    }

    public void warmUp() {
        warmUpLegacyModels();
        externalModel.warmUp();
    }

    private void warmUpLegacyModels() {
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
            attachmentModel = ClientAttachmentIndex.getOrLoadAttachmentModel(display.getModel());
            if (attachmentModel != null) {
                attachmentModel.setIsScope(display.isScope());
                attachmentModel.setIsSight(display.isSight());
            }
            modelTexture = display.getTexture();
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
            GunMod.LOGGER.warn("Failed to load attachment variant model {}", display.getModel(), throwable);
        }
    }

    public Component getName() {
        return name;
    }

    public @Nullable BedrockAttachmentModel getAttachmentModel() {
        ensureModelsLoaded();
        return attachmentModel;
    }

    public @Nullable ExternalGunModel getExternalModel() {
        return externalModel.getIfReady();
    }

    private @Nullable ExternalGunModel bakeExternalModel() {
        if (modelsLoadFailed) {
            return null;
        }

        try {
            loadModelsIfNecessary();
        } catch (RuntimeException failure) {
            handleModelsLoadFailure(failure);
            return null;
        }

        if (attachmentModel == null || display.getModel() == null) {
            return null;
        }

        BedrockModelPOJO source = ClientAssetsManager.INSTANCE.getBedrockModelPOJO(display.getModel());
        if (source == null) {
            return null;
        }
        Set<String> hiddenTextBones = display.getTextShows() == null
                ? Set.of() : Set.copyOf(display.getTextShows().keySet());
        return ExternalGunModel.bake(source, hiddenTextBones, DisplayGunModelInstance.Kind.ATTACHMENT);
    }

    public ExternalModelAssets getExternalModels() {
        return externalModels;
    }

    public @Nullable ResourceLocation getModelTexture() {
        ensureModelsLoaded();
        return modelTexture;
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

    public boolean isScope() {
        return scope;
    }

    public boolean isSight() {
        return sight;
    }

    public @Nullable Vector3f getMountOffset() {
        return mountOffset;
    }

    public boolean isReplaceMode() {
        return !"overlay".equalsIgnoreCase(mode);
    }

    public boolean isOverlayMode() {
        return "overlay".equalsIgnoreCase(mode);
    }
}
