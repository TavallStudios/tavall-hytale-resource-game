package com.tavall.hytale.resourcegame.dependency.interfaces;

import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableInterface;
import com.tavall.hytale.resourcegame.ui.UiPageFactory;
import com.tavall.hytale.resourcegame.ui.UiPageType;

public interface IUiPageRegistry extends IDependencyInjectableInterface {
    void register(UiPageType type, UiPageFactory factory);

    UiPageFactory get(UiPageType type);
}