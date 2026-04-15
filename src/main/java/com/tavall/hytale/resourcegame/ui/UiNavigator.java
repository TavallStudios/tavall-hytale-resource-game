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
    private static final Set<UiPageType> REFRESHABLE_PAGE_TYPES = Set.of(
            UiPageType.CASTLE_MAIN,
            UiPageType.CASTLE_CITIZENS,
            UiPageType.CASTLE_RESOURCES,
            UiPageType.CASTLE_UPGRADES,
            UiPageType.RESOURCE_NODE_DETAIL
    );

    private final IUiPageRegistry registry;
    private final ConcurrentHashMap<UUID, TrackedUiState> trackedPages = new ConcurrentHashMap<>();

    public UiNavigator(IUiPageRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    public void open(UiPageType type, Player player, UiNavigationContext context, PlayerGameState state) {
        trackedPages.put(player.getUuid(), new TrackedUiState(type, context));
        open(type, player, context, state, MAX_RETRIES);
    }

    @Override
    public void refreshTrackedPage(UUID playerId, PlayerGameState state) {
        TrackedUiState trackedUiState = trackedPages.get(playerId);
        if (trackedUiState == null || !REFRESHABLE_PAGE_TYPES.contains(trackedUiState.pageType())) {
            return;
        }
        Player livePlayer = resolveOnlinePlayer(playerId);
        if (livePlayer == null) {
            trackedPages.remove(playerId);
            return;
        }
        open(trackedUiState.pageType(), livePlayer, trackedUiState.navigationContext(), state, MAX_RETRIES);
    }

    @Override
    public void clearTrackedPage(UUID playerId) {
        if (playerId != null) {
            trackedPages.remove(playerId);
        }
    }

    private void open(UiPageType type, Player player, UiNavigationContext context, PlayerGameState state, int retriesRemaining) {
        Ref<EntityStore> ref = player.getPlayerRef().getReference();
        if (ref != null && ref.isValid()) {
            Store<EntityStore> store = ref.getStore();
            World world = ((EntityStore) store.getExternalData()).getWorld();
            world.execute(() -> openOnWorld(type, player, context, state));
            return;
        }
        if (player.getWorld() != null) {
            player.getWorld().execute(() -> openOnWorld(type, player, context, state));
            return;
        }
        if (retriesRemaining > 0) {
            HytaleServer.SCHEDULED_EXECUTOR.schedule(
                    () -> open(type, player, context, state, retriesRemaining - 1),
                    RETRY_DELAY_MILLIS,
                    TimeUnit.MILLISECONDS
            );
            return;
        }
        LOGGER.at(Level.WARNING).log("UI open skipped for %s because player world is not available yet.", type);
    }

    private void openOnWorld(UiPageType type, Player player, UiNavigationContext context, PlayerGameState state) {
        UiPageFactory factory = registry.get(type);
        if (factory == null) {
            LOGGER.at(Level.WARNING).log("UI open skipped for %s because no factory is registered.", type);
            return;
        }
        Ref<EntityStore> ref = player.getPlayerRef().getReference();
        if (ref == null || !ref.isValid()) {
            LOGGER.at(Level.WARNING).log("UI open skipped for %s because player ref is not valid.", type);
            return;
        }
        Store<EntityStore> store = ref.getStore();
        Player livePlayer = store.getComponent(ref, Player.getComponentType());
        if (livePlayer == null) {
            LOGGER.at(Level.WARNING).log("UI open skipped for %s because live player component is not available.", type);
            return;
        }
        BaseUiPage page = factory.create(livePlayer, context, state);
        LOGGER.at(Level.INFO).log("Opening UI page %s for %s.", type, livePlayer.getDisplayName());
        livePlayer.getPageManager().openCustomPage(ref, store, page);
    }

    private Player resolveOnlinePlayer(UUID playerId) {
        for (PlayerRef playerRef : Universe.get().getPlayers()) {
            if (!playerRef.getUuid().equals(playerId)) {
                continue;
            }
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref == null || !ref.isValid()) {
                return null;
            }
            return ref.getStore().getComponent(ref, Player.getComponentType());
        }
        return null;
    }
}
