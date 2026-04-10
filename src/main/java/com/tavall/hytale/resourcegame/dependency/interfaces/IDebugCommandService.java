package com.tavall.hytale.resourcegame.dependency.interfaces;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableInterface;

import java.util.List;

public interface IDebugCommandService extends IDependencyInjectableInterface {
    List<AbstractAsyncCommand> commands();
}