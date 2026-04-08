package com.tavall.hytale.resourcegame.tasks;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Names async worker threads for diagnostics.
 */
public final class AsyncTaskThreadFactory implements ThreadFactory {
    private final AtomicInteger counter = new AtomicInteger(1);

    @Override
    public Thread newThread(Runnable runnable) {
        Thread thread = Thread.ofVirtual().name("resource-game-async-" + counter.getAndIncrement()).unstarted(runnable);
        thread.setDaemon(true);
        return thread;
    }
}
