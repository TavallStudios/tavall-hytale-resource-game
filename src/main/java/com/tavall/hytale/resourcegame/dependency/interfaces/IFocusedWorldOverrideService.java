package com.tavall.hytale.resourcegame.dependency.interfaces;

import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableInterface;
import com.tavall.hytale.resourcegame.domain.FocusedWorldTarget;
import java.util.Optional;
import java.util.UUID;

/**
 * Tracks short-lived explicit focus overrides for interaction-first command flows.
 */
public interface IFocusedWorldOverrideService extends IDependencyInjectableInterface {
    void markCastle(UUID playerId);

    void markNode(UUID playerId, UUID nodeId);

    void markBuilding(UUID playerId, UUID buildingId);

    Optional<FocusedWorldTarget> peek(UUID playerId);

    Optional<FocusedWorldTarget> consume(UUID playerId);

    void clear(UUID playerId);
}
