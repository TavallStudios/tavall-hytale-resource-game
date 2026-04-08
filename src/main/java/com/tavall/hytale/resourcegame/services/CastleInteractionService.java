package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.tavall.hytale.resourcegame.config.CastleAssetConfig;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.domain.UiNavigationContext;
import com.tavall.hytale.resourcegame.ui.UiPageType;
import com.tavall.hytale.resourcegame.ui.UiNavigator;
import com.tavall.hytale.resourcegame.world.CastleEntityRegistry;
import com.tavall.hytale.resourcegame.world.VectorMath;

import java.util.Objects;
import java.util.UUID;

/**
 * Detects castle interactions and opens the main UI.
 */
public final class CastleInteractionService {
    private final CastleEntityRegistry registry;
    private final PlayerSessionStore sessionStore;
    private final UiNavigator uiNavigator;
    private final CastleAssetConfig assetConfig;

    public CastleInteractionService(
            CastleEntityRegistry registry,
            PlayerSessionStore sessionStore,
            UiNavigator uiNavigator,
            CastleAssetConfig assetConfig
    ) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore");
        this.uiNavigator = Objects.requireNonNull(uiNavigator, "uiNavigator");
        this.assetConfig = Objects.requireNonNull(assetConfig, "assetConfig");
    }

    public void handleInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!isOwnedCastleTarget(player, event.getTargetRef())) {
            return;
        }
        openCastleUi(player);
    }

    public boolean isPlayerFocusingOwnedCastle(Player player) {
        Vector3d castlePos = resolveCastlePosition(player);
        if (castlePos == null) {
            return false;
        }
        TransformComponent playerTransform = player.getTransformComponent();
        if (playerTransform == null) {
            return false;
        }
        Vector3d playerPos = playerTransform.getPosition();
        double distance = playerPos.distanceTo(castlePos);
        if (distance > assetConfig.interactionDistance()) {
            return false;
        }
        Vector3f rotation = playerTransform.getRotation();
        Vector3d lookVector = VectorMath.lookVector(rotation);
        Vector3d toCastle = new Vector3d(castlePos.getX() - playerPos.getX(), castlePos.getY() - playerPos.getY(), castlePos.getZ() - playerPos.getZ());
        Vector3d toCastleNorm = VectorMath.normalize(toCastle);
        double dot = VectorMath.dot(lookVector, toCastleNorm);
        return dot >= assetConfig.lookDotThreshold();
    }

    public void openCastleUi(Player player) {
        UUID playerId = player.getUuid();
        PlayerSession session = sessionStore.get(playerId);
        if (session == null) {
            return;
        }
        PlayerGameState state = session.gameState();
        uiNavigator.open(UiPageType.CASTLE_MAIN, player, new UiNavigationContext(playerId, player.getDisplayName()), state);
    }

    private boolean isOwnedCastleTarget(Player player, Ref<EntityStore> targetRef) {
        UUID playerId = player.getUuid();
        if (targetRef == null || !targetRef.isValid()) {
            return false;
        }
        Ref<EntityStore> castleRef = registry.get(playerId);
        return castleRef != null && castleRef.equals(targetRef);
    }

    private Vector3d resolveCastlePosition(Player player) {
        PlayerSession session = sessionStore.get(player.getUuid());
        if (session == null || session.gameState().castleLocation() == null) {
            return null;
        }
        Ref<EntityStore> castleRef = registry.get(player.getUuid());
        if (castleRef != null && castleRef.isValid()) {
            Store<EntityStore> store = castleRef.getStore();
            TransformComponent castleTransform = store.getComponent(castleRef, TransformComponent.getComponentType());
            if (castleTransform != null) {
                return castleTransform.getPosition();
            }
        }
        var castleLocation = session.gameState().castleLocation();
        return new Vector3d(castleLocation.x(), castleLocation.y(), castleLocation.z());
    }
}
