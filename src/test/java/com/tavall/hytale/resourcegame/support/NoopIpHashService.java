package com.tavall.hytale.resourcegame.support;

import com.tavall.hytale.resourcegame.dependency.interfaces.IIpHashService;

public final class NoopIpHashService implements IIpHashService {
    @Override
    public String hash(String rawValue) {
        return "";
    }
}

