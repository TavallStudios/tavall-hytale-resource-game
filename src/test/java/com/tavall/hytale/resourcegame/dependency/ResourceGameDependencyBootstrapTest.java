package com.tavall.hytale.resourcegame.dependency;

import com.tavall.hytale.resourcegame.ResourceGamePlugin;
import com.tavall.hytale.resourcegame.dependency.composition.domains.IResourceGameDomain;
import com.tavall.hytale.resourcegame.dependency.injection.helpers.DependencyInjectorHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class ResourceGameDependencyBootstrapTest {
    @AfterEach
    void clearRegistry() {
        DependencyLoaderAccess.clear();
    }

    @Test
    void helperRegistersDomainTokens() {
        TestResourceGameDependencyModule module = new TestResourceGameDependencyModule();
        DependencyInjectorHelper helper = new DependencyInjectorHelper();
        TestResourceGameDomain domain = new TestResourceGameDomain();

        helper.setupDISystem(module);

        assertSame(module.playerDataService(), domain.getPlayerDataService());
        assertSame(module.castleInteractionService(), domain.getCastleInteractionService());
        assertSame(module.castleProximityPromptService(), domain.getCastleProximityPromptService());
        assertSame(module.debugCommandService(), domain.getDebugCommandService());
        assertSame(module.kingdomClockService(), domain.getKingdomClockService());
    }

    @Test
    void pluginImplementsResourceGameDomain() {
        assertTrue(IResourceGameDomain.class.isAssignableFrom(ResourceGamePlugin.class));
    }
}