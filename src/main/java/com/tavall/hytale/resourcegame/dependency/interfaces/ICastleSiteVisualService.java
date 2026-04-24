package com.tavall.hytale.resourcegame.dependency.interfaces;

import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableInterface;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;

import java.util.UUID;

public interface ICastleSiteVisualService extends IDependencyInjectableInterface {
    void ensureSite(UUID playerId, PlayerGameState state);

    void refreshSite(UUID playerId, PlayerGameState state);

    void clearSite(UUID playerId);
}
