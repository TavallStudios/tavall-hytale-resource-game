package com.tavall.hytale.resourcegame.dependency.interfaces;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableInterface;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.domain.UiNavigationContext;
import com.tavall.hytale.resourcegame.ui.UiPageType;

import java.util.UUID;

public interface IUiNavigator extends IDependencyInjectableInterface {
    void open(UiPageType type, Player player, UiNavigationContext context, PlayerGameState state);

    void refreshTrackedPage(UUID playerId, PlayerGameState state);

    void clearTrackedPage(UUID playerId);
}
