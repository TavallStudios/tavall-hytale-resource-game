package com.tavall.hytale.resourcegame;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.tavall.hytale.resourcegame.dependency.composition.domains.IResourceGameDomain;
import com.tavall.hytale.resourcegame.dependency.injection.helpers.DependencyInjectorHelper;
import com.tavall.hytale.resourcegame.dependency.injection.helpers.interfaces.IDependencyInjectorHelper;
import com.tavall.hytale.resourcegame.dependency.modules.ResourceGameDependencyModule;
import com.tavall.hytale.resourcegame.tasks.AsyncTask;

import javax.annotation.Nonnull;
import java.util.List;

public class ResourceGamePlugin extends JavaPlugin implements IResourceGameDomain {
    private final IDependencyInjectorHelper injectorHelper = new DependencyInjectorHelper();

    public ResourceGamePlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        injectorHelper.setupDISystem(new ResourceGameDependencyModule(this));
        getLogger().atInfo().log("Kingdom clock initialized. Daytime: %s", getKingdomClockService().snapshot().isDay());
    }

    @Override
    protected void start() {
        getEventRegistry().registerGlobal(PlayerReadyEvent.class, getPlayerDataService()::handlePlayerReady);
        getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, getPlayerDataService()::handlePlayerDisconnect);
        getEventRegistry().registerGlobal(PlayerInteractEvent.class, getCastleInteractionService()::handleInteract);
        getCastleProximityPromptService().start();

        List<AbstractAsyncCommand> commands = getDebugCommandService().commands();
        for (AbstractAsyncCommand command : commands) {
            getCommandRegistry().registerCommand(command);
            getLogger().atInfo().log("Registered command /%s aliases=%s", command.getName(), command.getAliases());
        }
    }

    @Override
    protected void shutdown() {
        getCastleProximityPromptService().shutdown();
        AsyncTask.shutdown();
    }
}
