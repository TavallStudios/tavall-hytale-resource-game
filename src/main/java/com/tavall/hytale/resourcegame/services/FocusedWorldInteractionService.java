package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleBuildingService;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleInteractionService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IFocusedWorldInteractionService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerSessionStore;
import com.tavall.hytale.resourcegame.dependency.interfaces.IResourceNodeService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IUiNavigator;
import com.tavall.hytale.resourcegame.domain.FocusedWorldTarget;
import com.tavall.hytale.resourcegame.domain.FocusedWorldTargetType;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.domain.UiNavigationContext;
import com.tavall.hytale.resourcegame.ui.UiPageType;
import com.tavall.hytale.resourcegame.world.VectorMath;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Provides a single explicit interaction path for focused castle and node targets.
 */
public final class FocusedWorldInteractionService implements IFocusedWorldInteractionService, IDependencyInjectableConcrete {
    private final IPlayerSessionStore sessionStore;
    private final ICastleBuildingService buildingService;
    private final ICastleInteractionService castleInteractionService;
    private final IResourceNodeService resourceNodeService;
    private final IUiNavigator uiNavigator;
    private final FocusedWorldTargetPlanner planner;

    public FocusedWorldInteractionService(
            IPlayerSessionStore sessionStore,
            ICastleBuildingService buildingService,
            ICastleInteractionService castleInteractionService,
            IResourceNodeService resourceNodeService,
            IUiNavigator uiNavigator,
            FocusedWorldTargetPlanner planner
    ) {
        this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore");
        this.buildingService = Objects.requireNonNull(buildingService, "buildingService");
        this.castleInteractionService = Objects.requireNonNull(castleInteractionService, "castleInteractionService");
        this.resourceNodeService = Objects.requireNonNull(resourceNodeService, "resourceNodeService");
        this.uiNavigator = Objects.requireNonNull(uiNavigator, "uiNavigator");
        this.planner = Objects.requireNonNull(planner, "planner");
    }

    @Override
    public Optional<FocusedWorldTarget> resolve(Player player) {
        PlayerSession session = sessionStore.get(player.getUuid());
        if (session == null) {
            return Optional.empty();
        }
        TransformComponent transform = player.getTransformComponent();
        if (transform == null || player.getWorld() == null) {
            return Optional.empty();
        }
        Vector3d playerPosition = transform.getPosition();
        Vector3f rotation = transform.getRotation();
        Vector3d lookVector = VectorMath.lookVector(rotation);
        PlayerGameState state = session.gameState();
        return planner.resolve(
                player.getWorld().getName(),
                playerPosition,
                lookVector,
                state.castleLocation(),
                resourceNodeService.listNodes(state),
                buildingService.listBuildings(state).stream()
                        .map(building -> buildingService.summary(player.getUuid(), state, building, java.time.Instant.now()))
                        .toList()
        );
    }

    @Override
    public Optional<UUID> focusedNodeId(Player player) {
        return resolve(player)
                .filter(target -> target.type() == FocusedWorldTargetType.RESOURCE_NODE)
                .map(FocusedWorldTarget::nodeId);
    }

    @Override
    public Optional<UUID> focusedBuildingId(Player player) {
        return resolve(player)
                .filter(target -> target.type() == FocusedWorldTargetType.BUILDING)
                .map(FocusedWorldTarget::buildingId);
    }

    @Override
    public Optional<FocusedWorldTarget> interact(Player player) {
        Optional<FocusedWorldTarget> target = resolve(player);
        if (target.isEmpty()) {
            return Optional.empty();
        }
        PlayerSession session = sessionStore.get(player.getUuid());
        if (session == null) {
            return Optional.empty();
        }
        FocusedWorldTarget focusedTarget = target.get();
        if (focusedTarget.type() == FocusedWorldTargetType.CASTLE) {
            castleInteractionService.openCastleUi(player);
            return target;
        }
        if (focusedTarget.type() == FocusedWorldTargetType.BUILDING) {
            uiNavigator.open(
                    UiPageType.BUILDING_DETAIL,
                    player,
                    new UiNavigationContext(player.getUuid(), player.getDisplayName()).withSelectedBuildingId(focusedTarget.buildingId()),
                    session.gameState()
            );
            return target;
        }
        uiNavigator.open(
                UiPageType.RESOURCE_NODE_DETAIL,
                player,
                new UiNavigationContext(player.getUuid(), player.getDisplayName()).withSelectedNodeId(focusedTarget.nodeId()),
                session.gameState()
        );
        return target;
    }
}
