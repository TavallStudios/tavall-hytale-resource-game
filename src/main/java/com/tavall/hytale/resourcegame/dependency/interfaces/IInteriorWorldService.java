package com.tavall.hytale.resourcegame.dependency.interfaces;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableInterface;

public interface IInteriorWorldService extends IDependencyInjectableInterface {
    void enterInterior(Player player);

    void exitInterior(Player player);
}