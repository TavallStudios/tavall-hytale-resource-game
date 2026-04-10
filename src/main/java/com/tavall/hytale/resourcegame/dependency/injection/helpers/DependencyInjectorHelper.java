package com.tavall.hytale.resourcegame.dependency.injection.helpers;

import com.tavall.hytale.resourcegame.dependency.DependencyLoaderAccess;
import com.tavall.hytale.resourcegame.dependency.IDependencyModule;
import com.tavall.hytale.resourcegame.dependency.injection.helpers.interfaces.IDependencyInjectorHelper;

import java.util.Objects;

/**
 * Clears and repopulates the local dependency registry.
 */
public final class DependencyInjectorHelper implements IDependencyInjectorHelper {
    @Override
    public void setupDISystem(IDependencyModule module) {
        Objects.requireNonNull(module, "module");
        DependencyLoaderAccess.clear();
        module.registerDependencies();
    }
}