package com.tavall.hytale.resourcegame.dependency;

import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleInteractionService;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleProximityPromptService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IDebugCommandService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IKingdomClockService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerDataService;

/**
 * Minimal test composition root for the repo-local DI layer.
 */
public final class TestResourceGameDependencyModule implements IDependencyModule {
    private final TestPlayerDataService playerDataService = new TestPlayerDataService();
    private final TestCastleInteractionService castleInteractionService = new TestCastleInteractionService();
    private final TestCastleProximityPromptService castleProximityPromptService = new TestCastleProximityPromptService();
    private final TestDebugCommandService debugCommandService = new TestDebugCommandService();
    private final TestKingdomClockService kingdomClockService = new TestKingdomClockService();

    @Override
    public void registerDependencies() {
        DependencyLoaderAccess.registerInstance(IPlayerDataService.class, playerDataService);
        DependencyLoaderAccess.registerInstance(ICastleInteractionService.class, castleInteractionService);
        DependencyLoaderAccess.registerInstance(ICastleProximityPromptService.class, castleProximityPromptService);
        DependencyLoaderAccess.registerInstance(IDebugCommandService.class, debugCommandService);
        DependencyLoaderAccess.registerInstance(IKingdomClockService.class, kingdomClockService);
    }

    public TestPlayerDataService playerDataService() {
        return playerDataService;
    }

    public TestCastleInteractionService castleInteractionService() {
        return castleInteractionService;
    }

    public TestCastleProximityPromptService castleProximityPromptService() {
        return castleProximityPromptService;
    }

    public TestDebugCommandService debugCommandService() {
        return debugCommandService;
    }

    public TestKingdomClockService kingdomClockService() {
        return kingdomClockService;
    }
}