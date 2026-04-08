package com.tavall.hytale.resourcegame.support;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;

public final class TestAwait {
    private TestAwait() {
    }

    public static void until(BooleanSupplier condition, Duration timeout, String message) {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            if (condition.getAsBoolean()) {
                return;
            }
            LockSupport.parkNanos(Duration.ofMillis(10).toNanos());
        }
        if (!condition.getAsBoolean()) {
            throw new AssertionError(message);
        }
    }
}
