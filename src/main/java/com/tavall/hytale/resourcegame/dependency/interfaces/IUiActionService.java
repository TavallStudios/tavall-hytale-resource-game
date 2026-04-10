package com.tavall.hytale.resourcegame.dependency.interfaces;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableInterface;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.domain.UiNavigationContext;
import com.tavall.hytale.resourcegame.ui.UiActionEventData;
import com.tavall.hytale.resourcegame.ui.UpgradeActionState;

public interface IUiActionService extends IDependencyInjectableInterface {
    void handle(Player player, UiNavigationContext context, UiActionEventData eventData);

    UpgradeActionState promoteActionState(PlayerGameState state);

    UpgradeActionState demoteActionState(PlayerGameState state);

    String promotionCostSummary();
}