package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerSessionStore;
import com.tavall.hytale.resourcegame.dependency.interfaces.IUiNavigator;
import com.tavall.hytale.resourcegame.dependency.interfaces.IWorkerNpcInteractionService;
import com.tavall.hytale.resourcegame.domain.CitizenJobType;
import com.tavall.hytale.resourcegame.domain.UiNavigationContext;
import com.tavall.hytale.resourcegame.ui.UiPageType;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Opens worker-focused debug menus from stationary interior worker anchors.
 */
public final class WorkerNpcInteractionService implements IWorkerNpcInteractionService, IDependencyInjectableConcrete {
    private final PopulationDisplayService populationDisplayService;
    private final IPlayerSessionStore sessionStore;
    private final IUiNavigator uiNavigator;

    public WorkerNpcInteractionService(
            PopulationDisplayService populationDisplayService,
            IPlayerSessionStore sessionStore,
            IUiNavigator uiNavigator
    ) {
        this.populationDisplayService = Objects.requireNonNull(populationDisplayService, "populationDisplayService");
        this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore");
        this.uiNavigator = Objects.requireNonNull(uiNavigator, "uiNavigator");
    }

    @Override
    public void handleInteract(PlayerInteractEvent event) {
        if (event.isCancelled()) {
            return;
        }
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        UUID playerId = player.getUuid();
        PlayerSession session = sessionStore.get(playerId);
        if (session == null) {
            return;
        }
        Ref<EntityStore> targetRef = event.getTargetRef();
        Optional<CitizenJobType> workerType = populationDisplayService.resolveWorkerType(playerId, targetRef);
        if (workerType.isPresent()) {
            openWorkerPage(player, session, workerType.get());
            return;
        }
        if (populationDisplayService.isTroopAnchor(playerId, targetRef)) {
            uiNavigator.open(
                    UiPageType.CASTLE_TROOPS,
                    player,
                    new UiNavigationContext(playerId, player.getDisplayName()).withFeedback("Troop anchor selected. Might is tier-weighted; current aggregate troops are tier 1."),
                    session.gameState()
            );
        }
    }

    private void openWorkerPage(Player player, PlayerSession session, CitizenJobType workerType) {
        UiPageType pageType = workerType == CitizenJobType.SOLDIER || workerType == CitizenJobType.TRAINEE
                ? UiPageType.CASTLE_TROOPS
                : UiPageType.CASTLE_CITIZENS;
        uiNavigator.open(
                pageType,
                player,
                new UiNavigationContext(player.getUuid(), player.getDisplayName()).withFeedback(workerFeedback(workerType)),
                session.gameState()
        );
    }

    private String workerFeedback(CitizenJobType workerType) {
        return workerType.name() + " anchor selected. This stationary NPC remains while task copies leave through the portal.";
    }
}
