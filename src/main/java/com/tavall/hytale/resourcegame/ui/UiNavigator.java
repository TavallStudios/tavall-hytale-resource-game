package com.tavall.hytale.resourcegame.ui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.dependency.interfaces.IUiNavigator;
import com.tavall.hytale.resourcegame.dependency.interfaces.IUiPageRegistry;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.domain.TrackedUiState;
import com.tavall.hytale.resourcegame.domain.UiNavigationContext;
import com.tavall.hytale.resourcegame.tasks.WorldTasks;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Opens and updates UI pages for players.
 */
public final class UiNavigator implements IUiNavigator, IDependencyInjectableConcrete {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final int MAX_RETRIES = 40;
    private static final long RETRY_DELAY_MILLIS = 250L;
    private static final long DUPLICATE_OPEN_DEBOUNCE_MILLIS = 1000L;
    private static final Set<UiPageType> REFRESHABLE_PAGE_TYPES = Set.of(
            UiPageType.CASTLE_MAIN,
            UiPageType.CASTLE_CITIZENS,
            UiPageType.CASTLE_RESOURCES,
            UiPageType.CASTLE_UPGRADES,
            UiPageType.CASTLE_BUILDINGS,
            UiPageType.RESOURCE_NODE_DETAIL,
            UiPageType.BUILDING_DETAIL
    );

    private final IUiPageRegistry registry;
    private final ConcurrentHashMap<UUID, TrackedUiState> trackedPages = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> recentOpenTimes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, UiPageType> recentOpenTypes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String> recentOpenFingerprints = new ConcurrentHashMap<>();

    public UiNavigator(IUiPageRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    public void open(UiPageType type, Player player, UiNavigationContext context, PlayerGameState state) {
        try {
            UUID playerId = player.getUuid();
            TrackedUiState trackedState = new TrackedUiState(type, context, stateFingerprint(state));
            trackedPages.put(playerId, trackedState);
            open(trackedState, playerId, state, MAX_RETRIES);
        } catch (Throwable throwable) {
            Throwable rootCause = rootCause(throwable);
            LOGGER.at(Level.SEVERE).withCause(throwable).log(
                    "UI open failed for %s. cause=%s: %s",
                    type,
                    rootCause.getClass().getName(),
                    safeMessage(rootCause)
            );
        }
    }

    @Override
    public void refreshTrackedPage(UUID playerId, PlayerGameState state) {
        TrackedUiState trackedUiState = trackedPages.get(playerId);
        if (trackedUiState == null || !REFRESHABLE_PAGE_TYPES.contains(trackedUiState.pageType())) {
            return;
        }
        TrackedUiState refreshedState = new TrackedUiState(
                trackedUiState.pageType(),
                trackedUiState.navigationContext(),
                stateFingerprint(state)
        );
        trackedPages.put(playerId, refreshedState);
        open(refreshedState, playerId, state, MAX_RETRIES);
    }

    @Override
    public void clearTrackedPage(UUID playerId) {
        if (playerId != null) {
            trackedPages.remove(playerId);
            recentOpenTimes.remove(playerId);
            recentOpenTypes.remove(playerId);
            recentOpenFingerprints.remove(playerId);
        }
    }

    private void open(TrackedUiState trackedState, UUID playerId, PlayerGameState state, int retriesRemaining) {
        if (!isActiveRequest(playerId, trackedState)) {
            return;
        }
        Ref<EntityStore> ref = resolveOnlinePlayerRef(playerId);
        if (ref != null && ref.isValid()) {
            Store<EntityStore> store = ref.getStore();
            World world = ((EntityStore) store.getExternalData()).getWorld();
            if (world != null) {
                WorldTasks.executeSafe(world, "UiNavigator.openOnWorld(" + trackedState.pageType() + ")", () -> openOnWorld(trackedState, playerId, ref, store, state));
                return;
            }
        }
        if (retriesRemaining > 0) {
            HytaleServer.SCHEDULED_EXECUTOR.schedule(
                    () -> open(trackedState, playerId, state, retriesRemaining - 1),
                    RETRY_DELAY_MILLIS,
                    TimeUnit.MILLISECONDS
            );
            return;
        }
        LOGGER.at(Level.WARNING).log("UI open skipped for %s because player world is not available yet.", trackedState.pageType());
    }

    private void openOnWorld(
            TrackedUiState trackedState,
            UUID playerId,
            Ref<EntityStore> ref,
            Store<EntityStore> store,
            PlayerGameState state
    ) {
        if (!isActiveRequest(playerId, trackedState)) {
            return;
        }
        UiPageType type = trackedState.pageType();
        UiPageFactory factory = registry.get(type);
        if (factory == null) {
            LOGGER.at(Level.WARNING).log("UI open skipped for %s because no factory is registered.", type);
            return;
        }
        if (ref == null || !ref.isValid()) {
            LOGGER.at(Level.WARNING).log("UI open skipped for %s because player ref is not valid.", type);
            return;
        }
        try {
            if (!ref.isValid()) {
                LOGGER.at(Level.WARNING).log("UI open skipped for %s because player ref became invalid.", type);
                return;
            }
            Player livePlayer = store.getComponent(ref, Player.getComponentType());
            if (livePlayer == null) {
                LOGGER.at(Level.WARNING).log("UI open skipped for %s because live player component is not available.", type);
                return;
            }
            if (isDuplicateRecentOpen(playerId, trackedState)) {
                return;
            }
            BaseUiPage page = factory.create(livePlayer, trackedState.navigationContext(), state);
            LOGGER.at(Level.INFO).log("Opening UI page %s for %s.", type, livePlayer.getDisplayName());
            livePlayer.getPageManager().openCustomPage(ref, store, page);
            rememberRecentOpen(playerId, trackedState);
        } catch (Throwable throwable) {
            Throwable rootCause = rootCause(throwable);
            LOGGER.at(Level.SEVERE).withCause(rootCause).log(
                    "UI open failed for %s. cause=%s: %s",
                    type,
                    rootCause.getClass().getName(),
                    safeMessage(rootCause)
            );
        }
    }

    private boolean isActiveRequest(UUID playerId, TrackedUiState trackedState) {
        if (playerId == null || trackedState == null) {
            return false;
        }
        return trackedPages.get(playerId) == trackedState;
    }

    private boolean isDuplicateRecentOpen(UUID playerId, TrackedUiState trackedState) {
        if (playerId == null || trackedState == null) {
            return false;
        }
        Long openedAt = recentOpenTimes.get(playerId);
        if (openedAt == null || (System.currentTimeMillis() - openedAt) > DUPLICATE_OPEN_DEBOUNCE_MILLIS) {
            return false;
        }
        UiPageType recentType = recentOpenTypes.get(playerId);
        if (recentType != trackedState.pageType()) {
            return false;
        }
        String recentFingerprint = recentOpenFingerprints.get(playerId);
        return Objects.equals(recentFingerprint, contextFingerprint(trackedState));
    }

    private void rememberRecentOpen(UUID playerId, TrackedUiState trackedState) {
        if (playerId == null || trackedState == null) {
            return;
        }
        recentOpenTimes.put(playerId, System.currentTimeMillis());
        recentOpenTypes.put(playerId, trackedState.pageType());
        recentOpenFingerprints.put(playerId, contextFingerprint(trackedState));
    }

    private String contextFingerprint(TrackedUiState trackedState) {
        if (trackedState == null || trackedState.navigationContext() == null) {
            return "";
        }
        UiNavigationContext context = trackedState.navigationContext();
        return String.join(
                "|",
                String.valueOf(context.playerId()),
                String.valueOf(context.playerName()),
                String.valueOf(context.feedbackMessage()),
                String.valueOf(context.selectedNodeId()),
                String.valueOf(context.selectedBuildingId()),
                trackedState.stateFingerprint()
        );
    }

    private String stateFingerprint(PlayerGameState state) {
        if (state == null) {
            return "";
        }
        int metadataHash = state.metadataJson() == null ? 0 : state.metadataJson().hashCode();
        return String.join(
                "|",
                String.valueOf(state.updatedAt()),
                String.valueOf(state.resources().food()),
                String.valueOf(state.resources().wood()),
                String.valueOf(state.resources().iron()),
                String.valueOf(state.populationSummary().citizenCount()),
                String.valueOf(state.populationSummary().troopCount()),
                String.valueOf(metadataHash)
        );
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current != null && current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current == null ? new RuntimeException("unknown") : current;
    }

    private String safeMessage(Throwable throwable) {
        if (throwable == null || throwable.getMessage() == null || throwable.getMessage().isBlank()) {
            return "unknown error";
        }
        return throwable.getMessage();
    }

    private Ref<EntityStore> resolveOnlinePlayerRef(UUID playerId) {
        Universe universe = Universe.get();
        if (universe == null) {
            return null;
        }
        for (PlayerRef playerRef : universe.getPlayers()) {
            if (!playerRef.getUuid().equals(playerId)) {
                continue;
            }
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref == null || !ref.isValid()) {
                return null;
            }
            return ref;
        }
        return null;
    }

}
