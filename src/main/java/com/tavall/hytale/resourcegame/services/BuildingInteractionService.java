package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.dependency.interfaces.IBuildingInteractionService;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleBuildingVisualService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IFocusedWorldInteractionService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerSessionStore;
import com.tavall.hytale.resourcegame.dependency.interfaces.IUiNavigator;
import com.tavall.hytale.resourcegame.domain.UiNavigationContext;
import com.tavall.hytale.resourcegame.ui.UiPageType;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Opens the building detail UI when a player interacts with one of their building anchors.
 */
public final class BuildingInteractionService implements IBuildingInteractionService, IDependencyInjectableConcrete {
    private final IPlayerSessionStore sessionStore;
    private final ICastleBuildingVisualService buildingVisualService;
    private final IFocusedWorldInteractionService focusedWorldInteractionService;
    private final IUiNavigator uiNavigator;

    public BuildingInteractionService(
            IPlayerSessionStore sessionStore,
            ICastleBuildingVisualService buildingVisualService,
            IFocusedWorldInteractionService focusedWorldInteractionService,
            IUiNavigator uiNavigator
    ) {
        this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore");
        this.buildingVisualService = Objects.requireNonNull(buildingVisualService, "buildingVisualService");
        this.focusedWorldInteractionService = Objects.requireNonNull(focusedWorldInteractionService, "focusedWorldInteractionService");
        this.uiNavigator = Objects.requireNonNull(uiNavigator, "uiNavigator");
    }

    @Override
    public void handleInteract(PlayerInteractEvent event) {
        if (event.isCancelled()) {
            return;
        }
        Player player = event.getPlayer();
        UUID playerId = player.getUuid();
        PlayerSession session = sessionStore.get(playerId);
        if (session == null) {
            return;
        }
        Optional<UUID> buildingId = buildingVisualService.findBuildingId(playerId, event.getTargetRef());
        if (buildingId.isEmpty()) {
            buildingId = focusedWorldInteractionService.focusedBuildingId(player);
        }
        if (buildingId.isEmpty()) {
            return;
        }
        uiNavigator.open(
                UiPageType.BUILDING_DETAIL,
                player,
                new UiNavigationContext(playerId, player.getDisplayName()).withSelectedBuildingId(buildingId.get()),
                session.gameState()
        );
    }
}
