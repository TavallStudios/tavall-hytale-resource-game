package com.tavall.hytale.resourcegame.tasks;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * Lightweight async task runner for off-main-thread work.
 */
public final class AsyncTask {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(new AsyncTaskThreadFactory());

    private AsyncTask() {
    }

    public static CompletableFuture<Void> runAsync(Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable");
        return CompletableFuture.runAsync(runnable, EXECUTOR);
    }

    public static <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        return CompletableFuture.supplyAsync(supplier, EXECUTOR);
    }

    public static void shutdown() {
        EXECUTOR.shutdown();
    }
}
