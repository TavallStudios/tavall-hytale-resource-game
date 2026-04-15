package com.tavall.hytale.resourcegame.services;

import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleSiteVisualService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerGameStateService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerSessionStore;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPopulationService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IResourceService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IResourceNodeService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IResourceNodeVisualService;
import com.tavall.hytale.resourcegame.domain.AgingState;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.domain.PopulationSummary;
import com.tavall.hytale.resourcegame.domain.ResourceInventory;
import com.tavall.hytale.resourcegame.population.PromotionCost;
import com.tavall.hytale.resourcegame.ui.UpgradeActionState;

import com.tavall.hytale.resourcegame.tasks.AsyncTask;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Manages citizen and troop counts as a single continuum.
 */
public final class PopulationService implements IPopulationService, IDependencyInjectableConcrete {
    private final IPlayerSessionStore sessionStore;
    private final IPlayerGameStateService gameStateService;
    private final IResourceService resourceService;
    private final ICastleSiteVisualService castleSiteVisualService;
    private final PopulationDisplayGateway displayService;
    private final PromotionCost promotionCost;
    private final IResourceNodeService resourceNodeService;
    private final IResourceNodeVisualService resourceNodeVisualService;

    public PopulationService(
            IPlayerSessionStore sessionStore,
            IPlayerGameStateService gameStateService,
            IResourceService resourceService,
            ICastleSiteVisualService castleSiteVisualService,
            PopulationDisplayGateway displayService,
            PromotionCost promotionCost,
            IResourceNodeService resourceNodeService,
            IResourceNodeVisualService resourceNodeVisualService
    ) {
        this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore");
        this.gameStateService = Objects.requireNonNull(gameStateService, "gameStateService");
        this.resourceService = Objects.requireNonNull(resourceService, "resourceService");
        this.castleSiteVisualService = Objects.requireNonNull(castleSiteVisualService, "castleSiteVisualService");
        this.displayService = Objects.requireNonNull(displayService, "displayService");
        this.promotionCost = Objects.requireNonNull(promotionCost, "promotionCost");
        this.resourceNodeService = Objects.requireNonNull(resourceNodeService, "resourceNodeService");
        this.resourceNodeVisualService = Objects.requireNonNull(resourceNodeVisualService, "resourceNodeVisualService");
    }

    public PlayerGameState addCitizens(UUID playerId, int amount) {
        return updatePopulation(playerId, amount, 0);
    }

    public PlayerGameState setCitizens(UUID playerId, int count) {
        PlayerSession session = sessionStore.get(playerId);
        if (session == null) {
            return null;
        }
        int delta = count - session.gameState().populationSummary().citizenCount();
        return updatePopulation(playerId, delta, 0);
    }

    public PlayerGameState addTroops(UUID playerId, int amount) {
        return updatePopulation(playerId, 0, amount);
    }

    public PlayerGameState setTroops(UUID playerId, int count) {
        PlayerSession session = sessionStore.get(playerId);
        if (session == null) {
            return null;
        }
        int delta = count - session.gameState().populationSummary().troopCount();
        return updatePopulation(playerId, 0, delta);
    }

    public boolean promoteCitizen(UUID playerId) {
        PlayerSession session = sessionStore.get(playerId);
        if (session == null) {
            return false;
        }
        PopulationSummary summary = session.gameState().populationSummary();
        if (summary.citizenCount() <= 0) {
            return false;
        }
        ResourceInventory resources = session.gameState().resources();
        if (resources.food() < promotionCost.foodCost()
                || resources.wood() < promotionCost.woodCost()
                || resources.iron() < promotionCost.ironCost()) {
            return false;
        }
        resourceService.setResource(playerId, com.tavall.hytale.resourcegame.resources.ResourceType.FOOD, resources.food() - promotionCost.foodCost());
        resourceService.setResource(playerId, com.tavall.hytale.resourcegame.resources.ResourceType.WOOD, resources.wood() - promotionCost.woodCost());
        resourceService.setResource(playerId, com.tavall.hytale.resourcegame.resources.ResourceType.IRON, resources.iron() - promotionCost.ironCost());
        updatePopulation(playerId, -1, 1);
        return true;
    }

    public boolean demoteTroop(UUID playerId) {
        PlayerSession session = sessionStore.get(playerId);
        if (session == null) {
            return false;
        }
        PopulationSummary summary = session.gameState().populationSummary();
        if (summary.troopCount() <= 0) {
            return false;
        }
        updatePopulation(playerId, 1, -1);
        return true;
    }

    public UpgradeActionState promoteActionState(PlayerGameState state) {
        PopulationSummary summary = state.populationSummary();
        if (summary.citizenCount() <= 0) {
            return new UpgradeActionState(false, "Blocked: need at least 1 citizen.");
        }
        ResourceInventory resources = state.resources();
        if (resources.food() < promotionCost.foodCost()) {
            return new UpgradeActionState(false, "Blocked: need " + promotionCost.foodCost() + " Food.");
        }
        if (resources.wood() < promotionCost.woodCost()) {
            return new UpgradeActionState(false, "Blocked: need " + promotionCost.woodCost() + " Wood.");
        }
        if (resources.iron() < promotionCost.ironCost()) {
            return new UpgradeActionState(false, "Blocked: need " + promotionCost.ironCost() + " Iron.");
        }
        return new UpgradeActionState(true, "Ready: promote 1 citizen into 1 troop.");
    }

    public UpgradeActionState demoteActionState(PlayerGameState state) {
        if (state.populationSummary().troopCount() <= 0) {
            return new UpgradeActionState(false, "Blocked: need at least 1 troop.");
        }
        return new UpgradeActionState(true, "Ready: return 1 troop to the citizen pool.");
    }

    public String promotionCostSummary() {
        return "Cost per promotion: "
                + promotionCost.foodCost() + " Food, "
                + promotionCost.woodCost() + " Wood, "
                + promotionCost.ironCost() + " Iron.";
    }

    public PlayerGameState updateAging(UUID playerId, Instant now) {
        PlayerSession session = sessionStore.get(playerId);
        if (session == null) {
            return null;
        }
        PopulationSummary summary = session.gameState().populationSummary();
        AgingState aged = summary.agingState().tick(now);
        PopulationSummary updatedSummary = summary.withAgingState(aged);
        PlayerGameState updated = session.gameState().withPopulation(updatedSummary, now);
        session.updateGameState(updated);
        gameStateService.cacheState(playerId, updated);
        AsyncTask.runAsync(() -> gameStateService.persistState(updated, now));
        return updated;
    }

    private PlayerGameState updatePopulation(UUID playerId, int citizenDelta, int troopDelta) {
        PlayerSession session = sessionStore.get(playerId);
        if (session == null) {
            return null;
        }
        PopulationSummary summary = session.gameState().populationSummary();
        int citizens = Math.max(0, summary.citizenCount() + citizenDelta);
        int troops = Math.max(0, summary.troopCount() + troopDelta);
        PopulationSummary updatedSummary = new PopulationSummary(
                citizens,
                troops,
                summary.citizenMetaData(),
                summary.troopMetaData(),
                summary.agingState()
        );
        Instant now = Instant.now();
        PlayerGameState updated = session.gameState().withPopulation(updatedSummary, now);
        updated = resourceNodeService.normalizeAssignments(updated, now);
        session.updateGameState(updated);
        displayService.updateDisplays(playerId, updatedSummary);
        castleSiteVisualService.refreshSite(playerId, updated);
        resourceNodeVisualService.refreshNodes(playerId, updated);
        gameStateService.cacheState(playerId, updated);
        PlayerGameState persistedState = updated;
        AsyncTask.runAsync(() -> gameStateService.persistState(persistedState, now));
        return updated;
    }
}
