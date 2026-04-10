package com.tavall.hytale.resourcegame.dependency.interfaces;

import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableInterface;

public interface IIpHashService extends IDependencyInjectableInterface {
    String hash(String rawValue);
}