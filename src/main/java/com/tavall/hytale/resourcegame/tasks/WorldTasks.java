package com.tavall.hytale.resourcegame.tasks;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.World;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Helper for scheduling world-thread work while ensuring plugin exceptions do not crash the world thread.
 */
public final class WorldTasks {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final Logger JUL_LOGGER = Logger.getLogger(WorldTasks.class.getName());
    private static final int DEFAULT_FRAME_LIMIT = 10;

    private WorldTasks() {
    }

    public static void executeSafe(World world, String taskName, Runnable task) {
        if (world == null || task == null) {
            return;
        }
        world.execute(() -> runSafe(world, taskName, task));
    }

    public static void runSafe(World world, String taskName, Runnable task) {
        if (task == null) {
            return;
        }
        try {
            task.run();
        } catch (Throwable throwable) {
            String worldName = world == null ? "<null>" : world.getName();
            String name = taskName == null || taskName.isBlank() ? "<unnamed>" : taskName;
            String throwableSummary = throwable.getClass().getName() + ": " + safeMessage(throwable);
            String frames = stackFramesSummary(throwable, DEFAULT_FRAME_LIMIT);
            LOGGER.at(Level.SEVERE).log(
                    "World task failed: %s (world=%s thread=%s) cause=%s frames=%s",
                    name,
                    worldName,
                    Thread.currentThread().getName(),
                    throwableSummary,
                    frames
            );
            JUL_LOGGER.log(Level.SEVERE, "World task failed: " + name + " (world=" + worldName + " thread=" + Thread.currentThread().getName() + ")", throwable);
        }
    }

    private static String safeMessage(Throwable throwable) {
        if (throwable == null) {
            return "unknown";
        }
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            return "unknown";
        }
        return message;
    }

    private static String stackFramesSummary(Throwable throwable, int maxFrames) {
        if (throwable == null) {
            return "";
        }
        StackTraceElement[] trace = throwable.getStackTrace();
        if (trace == null || trace.length == 0) {
            return "";
        }
        int limit = Math.max(1, maxFrames);
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < trace.length && index < limit; index++) {
            if (index > 0) {
                builder.append(" | ");
            }
            builder.append(trace[index]);
        }
        Throwable cause = throwable.getCause();
        if (cause != null && cause != throwable) {
            builder.append(" || causedBy ").append(cause.getClass().getName()).append(": ").append(safeMessage(cause));
        }
        return builder.toString();
    }

}
