package com.tavall.hytale.resourcegame.dependency.composition.domains;

import com.tavall.hytale.resourcegame.dependency.DependencyLoaderAccess;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleInteractionService;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleEconomySimulationService;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleProximityPromptService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IDebugCommandService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IKingdomClockService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerDataService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IResourceNodeInteractionService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IResourceNodeVisualPulseService;

/**
 * Generated-domain equivalent for repo-local DI accessors.
 */
public interface IResourceGameDomainGenerated {
    default IPlayerDataService getPlayerDataService() {
        return DependencyLoaderAccess.findInstance(IPlayerDataService.class);
    }

    default ICastleInteractionService getCastleInteractionService() {
        return DependencyLoaderAccess.findInstance(ICastleInteractionService.class);
    }

    default ICastleProximityPromptService getCastleProximityPromptService() {
        return DependencyLoaderAccess.findInstance(ICastleProximityPromptService.class);
    }

    default ICastleEconomySimulationService getCastleEconomySimulationService() {
        return DependencyLoaderAccess.findInstance(ICastleEconomySimulationService.class);
    }

    default IResourceNodeInteractionService getResourceNodeInteractionService() {
        return DependencyLoaderAccess.findInstance(IResourceNodeInteractionService.class);
    }

    default IResourceNodeVisualPulseService getResourceNodeVisualPulseService() {
        return DependencyLoaderAccess.findInstance(IResourceNodeVisualPulseService.class);
    }

    default IDebugCommandService getDebugCommandService() {
        return DependencyLoaderAccess.findInstance(IDebugCommandService.class);
    }

    default IKingdomClockService getKingdomClockService() {
        return DependencyLoaderAccess.findInstance(IKingdomClockService.class);
    }
}
