package com.tacz.guns.client.resource;

import com.tacz.guns.GunMod;
import com.tacz.guns.config.client.ResourceConfig;
import net.minecraft.resources.ResourceLocation;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;

// 资源后台预热器
public final class ClientAssetLoadDispatcher {
    private static final AtomicLong GENERATION = new AtomicLong();
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "tacz-client-asset-preload");
        thread.setDaemon(true);
        return thread;
    });

    private ClientAssetLoadDispatcher() {
    }

    public static ExecutorService executor() {
        return EXECUTOR;
    }

    static <T> ClientAssetLoadTask<T> task(Supplier<T> loader, Consumer<Throwable> failed) {
        long generation = GENERATION.get();
        return new ClientAssetLoadTask<>(EXECUTOR, loader, () -> GENERATION.get() == generation, failed);
    }

    /** 跳过旧资源代次中尚未开始的任务；已经开始的任务也不能发布旧代结果。 */
    public static void invalidate() {
        GENERATION.incrementAndGet();
    }

    static CompletableFuture<?> preload(ClientAssetLoadTask<?> task, ResourceLocation modelId) {
        if (modelId != null && (!ResourceConfig.ENABLE_LAZY_CLIENT_ASSET_LOAD.get()
                || GunMod.MOD_ID.equals(modelId.getNamespace()))) {
            return task.warmUp();
        }
        return CompletableFuture.completedFuture(null);
    }
}
