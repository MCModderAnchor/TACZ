package com.tacz.guns.client.resource.index;

import com.google.common.base.Preconditions;
import com.tacz.guns.GunMod;
import com.tacz.guns.client.model.BedrockAttachmentModel;
import com.tacz.guns.client.resource.ClientAssetLoadDispatcher;
import com.tacz.guns.client.resource.ClientAssetsManager;
import com.tacz.guns.client.resource.pojo.display.attachment.AttachmentDisplay;
import com.tacz.guns.resource.index.CommonSlotAdapterIndex;
import com.tacz.guns.resource.pojo.SlotAdapterIndexPOJO;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class ClientSlotAdapterIndex {
    private final Object modelLoadLock = new Object();
    private Component name;
    private AttachmentDisplay display;
    private @Nullable BedrockAttachmentModel adapterModel;
    private @Nullable ResourceLocation modelTexture;
    private @Nullable Vector3f mountOffset;
    private volatile boolean modelsLoaded = false;
    private volatile boolean modelsLoadFailed = false;
    private volatile CompletableFuture<Void> warmUpTask = null;

    private ClientSlotAdapterIndex() {
    }

    public static ClientSlotAdapterIndex getInstance(CommonSlotAdapterIndex commonIndex) {
        ClientSlotAdapterIndex index = new ClientSlotAdapterIndex();
        SlotAdapterIndexPOJO pojo = commonIndex.getPojo();
        checkIndex(pojo, index);
        checkDisplay(pojo, index);
        return index;
    }

    private static void checkIndex(SlotAdapterIndexPOJO pojo, ClientSlotAdapterIndex index) {
        Preconditions.checkArgument(pojo != null, "index object file is empty");
        index.name = pojo.getName() == null ? Component.empty() : Component.translatable(pojo.getName());
    }

    private static void checkDisplay(SlotAdapterIndexPOJO pojo, ClientSlotAdapterIndex index) {
        ResourceLocation displayId = pojo.getDisplay();
        Preconditions.checkArgument(displayId != null, "index object missing display field");
        AttachmentDisplay display = ClientAssetsManager.INSTANCE.getSlotAdapterDisplay(displayId);
        Preconditions.checkArgument(display != null, "there is no corresponding display file");
        index.display = display;
        index.mountOffset = display.getMountOffset();
    }

    public void warmUp() {
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
            adapterModel = ClientAttachmentIndex.getOrLoadAttachmentModel(display.getModel());
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
            GunMod.LOGGER.warn("Failed to load slot adapter model {}", display.getModel(), throwable);
        }
    }

    public Component getName() {
        return name;
    }

    public @Nullable BedrockAttachmentModel getAdapterModel() {
        ensureModelsLoaded();
        return adapterModel;
    }

    public @Nullable ResourceLocation getModelTexture() {
        ensureModelsLoaded();
        return modelTexture;
    }

    public @Nullable Vector3f getMountOffset() {
        return mountOffset;
    }
}
