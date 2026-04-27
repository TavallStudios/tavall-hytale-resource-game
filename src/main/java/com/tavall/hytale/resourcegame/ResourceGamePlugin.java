package com.tavall.hytale.resourcegame;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.tavall.hytale.resourcegame.dependency.composition.domains.IResourceGameDomain;
import com.tavall.hytale.resourcegame.dependency.DependencyLoaderAccess;
import com.tavall.hytale.resourcegame.dependency.injection.helpers.DependencyInjectorHelper;
import com.tavall.hytale.resourcegame.dependency.injection.helpers.interfaces.IDependencyInjectorHelper;
import com.tavall.hytale.resourcegame.dependency.modules.ResourceGameDependencyModule;
import com.tavall.hytale.resourcegame.services.VisualVerificationControlService;
import com.tavall.hytale.resourcegame.tasks.AsyncTask;

import javax.annotation.Nonnull;
import java.util.List;

public class ResourceGamePlugin extends JavaPlugin implements IResourceGameDomain {
    private final IDependencyInjectorHelper injectorHelper = new DependencyInjectorHelper();
    private static volatile boolean uncaughtHandlerInstalled = false;

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
        installUncaughtExceptionHandler();
        getEventRegistry().registerGlobal(PlayerReadyEvent.class, getPlayerDataService()::handlePlayerReady);
        getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, getPlayerDataService()::handlePlayerDisconnect);
        getEventRegistry().registerGlobal(PlayerInteractEvent.class, getPlacementInteractionService()::handleInteract);
        getEventRegistry().registerGlobal(PlayerInteractEvent.class, getCastleInteractionService()::handleInteract);
        getEventRegistry().registerGlobal(PlayerInteractEvent.class, getResourceNodeInteractionService()::handleInteract);
        getEventRegistry().registerGlobal(PlayerInteractEvent.class, getBuildingInteractionService()::handleInteract);
        getEventRegistry().registerGlobal(PlayerInteractEvent.class, getWorkerNpcInteractionService()::handleInteract);
        getInteriorInstanceService().pruneTransientWorlds();
        getInteriorInstanceService().warmInteriorWorld().whenComplete((world, throwable) -> {
            if (throwable != null) {
                getLogger().atWarning().withCause(throwable).log("Failed to warm interior world during plugin startup.");
                return;
            }
            if (world != null) {
                getLogger().atInfo().log("Interior world warmed during plugin startup: %s", world.getName());
            }
        });
        getKingdomClockService().start();
        getCastleProximityPromptService().start();
        getCastleEconomySimulationService().start();
        getResourceNodeVisualPulseService().start();
        getProtectedBlockSystemService().start();
        DependencyLoaderAccess.findInstance(VisualVerificationControlService.class).start();

        List<AbstractAsyncCommand> commands = getDebugCommandService().commands();
        for (AbstractAsyncCommand command : commands) {
            getCommandRegistry().registerCommand(command);
            getLogger().atInfo().log("Registered command /%s aliases=%s", command.getName(), command.getAliases());
        }
    }

    private void installUncaughtExceptionHandler() {
        if (uncaughtHandlerInstalled) {
            return;
        }
        uncaughtHandlerInstalled = true;
        Thread.UncaughtExceptionHandler existing = Thread.getDefaultUncaughtExceptionHandler();
        Thread.UncaughtExceptionHandler handler = (thread, throwable) -> {
            try {
                getLogger().atSevere().withCause(throwable).log("Uncaught exception on thread %s", thread.getName());
                System.err.println("[ResourceGamePlugin] Uncaught exception on thread " + thread.getName());
                throwable.printStackTrace(System.err);
            } catch (Throwable ignored) {
            }
            if (existing != null) {
                try {
                    existing.uncaughtException(thread, throwable);
                } catch (Throwable ignored) {
                }
            }
        };
        Thread.setDefaultUncaughtExceptionHandler(handler);
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            if (thread == null || thread == Thread.currentThread()) {
                continue;
            }
            String threadName = thread.getName();
            if (threadName == null || (!threadName.startsWith("WorldThread") && !"main".equals(threadName))) {
                continue;
            }
            try {
                thread.setUncaughtExceptionHandler(handler);
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    protected void shutdown() {
        getCastleProximityPromptService().shutdown();
        getCastleEconomySimulationService().shutdown();
        getResourceNodeVisualPulseService().shutdown();
        getProtectedBlockSystemService().shutdown();
        DependencyLoaderAccess.findInstance(VisualVerificationControlService.class).shutdown();
        getKingdomClockService().shutdown();
        AsyncTask.shutdown();
    }
}
