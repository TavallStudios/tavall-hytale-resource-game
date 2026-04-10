package com.tavall.hytale.resourcegame.dependency;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Minimal repo-local dependency registry that mirrors the shared Tavall DI lookup style.
 */
public final class DependencyLoaderAccess {
    private static final Map<Class<?>, Object> INSTANCES = new ConcurrentHashMap<>();

    private DependencyLoaderAccess() {
    }

    public static void clear() {
        INSTANCES.clear();
    }

    public static <T> void registerInstance(Class<T> token, T instance) {
        Objects.requireNonNull(token, "token");
        Objects.requireNonNull(instance, "instance");
        INSTANCES.put(token, instance);
    }

    public static <T> T findInstance(Class<T> token) {
        Objects.requireNonNull(token, "token");
        Object instance = INSTANCES.get(token);
        if (instance == null) {
            throw new IllegalStateException("No dependency registered for token: " + token.getName());
        }
        return token.cast(instance);
    }
}