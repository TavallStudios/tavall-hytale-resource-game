package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerSessionStore;
import com.tavall.hytale.resourcegame.dependency.interfaces.IResourceNodeInteractionService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IResourceNodeVisualService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IUiNavigator;
import com.tavall.hytale.resourcegame.domain.UiNavigationContext;
import com.tavall.hytale.resourcegame.ui.UiPageType;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Opens the node detail UI when a player interacts with one of their node anchors.
 */
public final class ResourceNodeInteractionService implements IResourceNodeInteractionService, IDependencyInjectableConcrete {
    private final IPlayerSessionStore sessionStore;
    private final IResourceNodeVisualService resourceNodeVisualService;
    private final IUiNavigator uiNavigator;

    public ResourceNodeInteractionService(
            IPlayerSessionStore sessionStore,
            IResourceNodeVisualService resourceNodeVisualService,
            IUiNavigator uiNavigator
    ) {
        this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore");
        this.resourceNodeVisualService = Objects.requireNonNull(resourceNodeVisualService, "resourceNodeVisualService");
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
        Optional<UUID> nodeId = resourceNodeVisualService.findNodeId(playerId, event.getTargetRef());
        if (nodeId.isEmpty()) {
            return;
        }
        uiNavigator.open(
                UiPageType.RESOURCE_NODE_DETAIL,
                player,
                new UiNavigationContext(playerId, player.getDisplayName()).withSelectedNodeId(nodeId.get()),
                session.gameState()
        );
    }
}
