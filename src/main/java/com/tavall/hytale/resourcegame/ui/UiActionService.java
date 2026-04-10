package com.tavall.hytale.resourcegame.ui;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.dependency.interfaces.IInteriorWorldService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerSessionStore;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPopulationService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IUiActionService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IUiNavigator;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.domain.UiNavigationContext;
import com.tavall.hytale.resourcegame.services.PlayerSession;
import java.util.Objects;
import java.util.UUID;

/**
 * Routes UI actions to services.
 */
public final class UiActionService implements IUiActionService, IDependencyInjectableConcrete {
    private final IUiNavigator uiNavigator;
    private final IInteriorWorldService interiorWorldService;
    private final IPopulationService populationService;
    private final IPlayerSessionStore sessionStore;

    public UiActionService(
            IUiNavigator uiNavigator,
            IInteriorWorldService interiorWorldService,
            IPopulationService populationService,
            IPlayerSessionStore sessionStore
    ) {
        this.uiNavigator = Objects.requireNonNull(uiNavigator, "uiNavigator");
        this.interiorWorldService = Objects.requireNonNull(interiorWorldService, "interiorWorldService");
        this.populationService = Objects.requireNonNull(populationService, "populationService");
        this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore");
    }

    public void handle(Player player, UiNavigationContext context, UiActionEventData eventData) {
        if (eventData == null || eventData.action() == null) {
            return;
        }
        String action = eventData.action();
        UUID playerId = context.playerId();
        PlayerSession session = sessionStore.get(playerId);
        PlayerGameState state = session == null ? null : session.gameState();
        if (UiActions.ENTER_INTERIOR.equals(action)) {
            interiorWorldService.enterInterior(player);
            return;
        }
        if (UiActions.EXIT_INTERIOR.equals(action)) {
            interiorWorldService.exitInterior(player);
            return;
        }
        if (UiActions.OPEN_CASTLE_INFO.equals(action) && state != null) {
            uiNavigator.open(UiPageType.CASTLE_INFO, player, context, state);
            return;
        }
        if (UiActions.OPEN_CITIZENS.equals(action) && state != null) {
            uiNavigator.open(UiPageType.CASTLE_CITIZENS, player, context, state);
            return;
        }
        if (UiActions.OPEN_TROOPS.equals(action) && state != null) {
            uiNavigator.open(UiPageType.CASTLE_TROOPS, player, context, state);
            return;
        }
        if (UiActions.OPEN_RESOURCES.equals(action) && state != null) {
            uiNavigator.open(UiPageType.CASTLE_RESOURCES, player, context, state);
            return;
        }
        if (UiActions.OPEN_UPGRADES.equals(action) && state != null) {
            uiNavigator.open(UiPageType.CASTLE_UPGRADES, player, context, state);
            return;
        }
        if (UiActions.OPEN_CASTLE_MAIN.equals(action) && state != null) {
            uiNavigator.open(UiPageType.CASTLE_MAIN, player, context, state);
            return;
        }
        if (UiActions.OPEN_DEBUG.equals(action) && state != null) {
            uiNavigator.open(UiPageType.DEBUG_NAVIGATOR, player, context, state);
            return;
        }
        if (UiActions.PROMOTE.equals(action)) {
            boolean promoted = populationService.promoteCitizen(playerId);
            PlayerSession updatedSession = sessionStore.get(playerId);
            if (updatedSession != null) {
                String feedback = promoted
                        ? "Promotion complete."
                        : populationService.promoteActionState(updatedSession.gameState()).message();
                uiNavigator.open(UiPageType.CASTLE_UPGRADES, player, context.withFeedback(feedback), updatedSession.gameState());
            }
            return;
        }
        if (UiActions.DEMOTE.equals(action)) {
            boolean demoted = populationService.demoteTroop(playerId);
            PlayerSession updatedSession = sessionStore.get(playerId);
            if (updatedSession != null) {
                String feedback = demoted
                        ? "Demotion complete."
                        : populationService.demoteActionState(updatedSession.gameState()).message();
                uiNavigator.open(UiPageType.CASTLE_UPGRADES, player, context.withFeedback(feedback), updatedSession.gameState());
            }
        }
    }

    public UpgradeActionState promoteActionState(PlayerGameState state) {
        return populationService.promoteActionState(state);
    }

    public UpgradeActionState demoteActionState(PlayerGameState state) {
        return populationService.demoteActionState(state);
    }

    public String promotionCostSummary() {
        return populationService.promotionCostSummary();
    }
}
