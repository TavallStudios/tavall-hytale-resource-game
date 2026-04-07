package org.tavall.hytale.resourcegame.concurrent;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * Lightweight async runner used for off-main-loop persistence and hydration work.
 */
public final class AsyncTask {

  private static final ExecutorService EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

  private AsyncTask() {
  }

  public static <T> CompletableFuture<T> supply(String taskName, Supplier<T> supplier) {
    Objects.requireNonNull(taskName, "taskName");
    Objects.requireNonNull(supplier, "supplier");
    return CompletableFuture.supplyAsync(supplier, EXECUTOR);
  }

  public static CompletableFuture<Void> run(String taskName, Runnable runnable) {
    Objects.requireNonNull(taskName, "taskName");
    Objects.requireNonNull(runnable, "runnable");
    return CompletableFuture.runAsync(runnable, EXECUTOR);
  }

  public static void shutdown() {
    EXECUTOR.shutdown();
  }
}
