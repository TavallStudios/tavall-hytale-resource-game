package com.tavall.hytale.resourcegame.dependency.interfaces;

import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableInterface;
import com.tavall.hytale.resourcegame.services.PlayerSession;

import java.util.UUID;

public interface IPlayerSessionStore extends IDependencyInjectableInterface {
    PlayerSession get(UUID playerId);

    void put(PlayerSession session);

    void remove(UUID playerId);
}