package com.tavall.hytale.resourcegame.ui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.domain.UiNavigationContext;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Opens and updates UI pages for players.
 */
public final class UiNavigator {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final int MAX_RETRIES = 40;
    private static final long RETRY_DELAY_MILLIS = 250L;

    private final UiPageRegistry registry;

    public UiNavigator(UiPageRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    public void open(UiPageType type, Player player, UiNavigationContext context, PlayerGameState state) {
        open(type, player, context, state, MAX_RETRIES);
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
}
