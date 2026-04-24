package com.tavall.hytale.resourcegame.ui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.tavall.hytale.resourcegame.dependency.interfaces.IUiActionService;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.domain.UiNavigationContext;

import java.util.Objects;
import java.util.logging.Level;

/**
 * Base class for resource game UI pages.
 */
public abstract class BaseUiPage extends InteractiveCustomUIPage<UiActionEventData> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final Player player;
    private final UiNavigationContext context;
    private final PlayerGameState state;
    private final IUiActionService actionService;

    protected BaseUiPage(
            Player player,
            UiNavigationContext context,
            PlayerGameState state,
            IUiActionService actionService
    ) {
        super(player.getPlayerRef(), CustomPageLifetime.CanDismiss, UiActionEventData.CODEC);
        this.player = Objects.requireNonNull(player, "player");
        this.context = Objects.requireNonNull(context, "context");
        this.state = Objects.requireNonNull(state, "state");
        this.actionService = Objects.requireNonNull(actionService, "actionService");
    }

    protected Player player() {
        return player;
    }

    protected UiNavigationContext context() {
        return context;
    }

    protected PlayerGameState state() {
        return state;
    }

    protected IUiActionService actionService() {
        return actionService;
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> entityRef, Store<EntityStore> entityStore, UiActionEventData eventData) {
        if (eventData == null || eventData.action() == null || eventData.action().isBlank()) {
            return;
        }
        String action = eventData.action();
        LOGGER.at(Level.INFO).log(
                "Handling UI action %s from %s on %s.",
                action,
                player.getDisplayName(),
                getClass().getSimpleName()
        );
        if (UiActions.CLOSE.equals(action)) {
            actionService.handleClose(player, context);
            close();
            return;
        }
        actionService.handle(player, context, eventData);
    }
}
