package com.tacz.guns.client.resource;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** 同一资产只提交一次后台任务，渲染时读取结果不会等待加载完成。 */
public final class ClientAssetLoadTask<T> {
    private final Executor executor;
    private final Supplier<T> loader;
    private final BooleanSupplier current;
    private final Consumer<Throwable> failed;
    private volatile CompletableFuture<T> task;

    ClientAssetLoadTask(Executor executor, Supplier<T> loader,
                        BooleanSupplier current, Consumer<Throwable> failed) {
        this.executor = executor;
        this.loader = loader;
        this.current = current;
        this.failed = failed;
    }

    public CompletableFuture<T> warmUp() {
        if (!current.getAsBoolean()) {
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<T> existing = task;
        if (existing != null) {
            return existing;
        }

        synchronized (this) {
            if (task == null) {
                CompletableFuture<T> loading = CompletableFuture.supplyAsync(() -> {
                    if (!current.getAsBoolean()) {
                        return null;
                    }
                    return loader.get();
                }, executor);

                task = loading.handle((value, failure) -> {
                    if (!current.getAsBoolean()) {
                        return null;
                    }
                    if (failure != null) {
                        Throwable cause = failure.getCause();
                        failed.accept(cause == null ? failure : cause);
                        return null;
                    }
                    return value;
                });
            }
            return task;
        }
    }

    /** 返回 null 表示尚未就绪或无法加载，两种状态由 isPending 区分。 */
    public T getIfReady() {
        CompletableFuture<T> future = warmUp();
        if (!current.getAsBoolean() || !future.isDone()
                || future.isCompletedExceptionally() || future.isCancelled()) {
            return null;
        }
        return future.getNow(null);
    }

    public boolean isPending() {
        CompletableFuture<T> future = task;
        return current.getAsBoolean() && (future == null || !future.isDone());
    }
}
