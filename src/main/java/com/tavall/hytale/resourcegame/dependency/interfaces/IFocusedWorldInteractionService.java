package com.tavall.hytale.resourcegame.dependency.interfaces;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableInterface;
import com.tavall.hytale.resourcegame.domain.FocusedWorldTarget;

import java.util.Optional;
import java.util.UUID;

public interface IFocusedWorldInteractionService extends IDependencyInjectableInterface {
    Optional<FocusedWorldTarget> resolve(Player player);

    Optional<UUID> focusedNodeId(Player player);

    Optional<FocusedWorldTarget> interact(Player player);
}
