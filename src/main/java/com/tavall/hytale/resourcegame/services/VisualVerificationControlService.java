package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerDataService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerSessionStore;
import com.tavall.hytale.resourcegame.dependency.interfaces.IUiNavigator;
import com.tavall.hytale.resourcegame.domain.UiNavigationContext;
import com.tavall.hytale.resourcegame.tasks.WorldTasks;
import com.tavall.hytale.resourcegame.ui.UiPageType;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public final class VisualVerificationControlService implements IDependencyInjectableConcrete {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final long POLL_INTERVAL_MILLIS = 250L;

    private final IPlayerDataService playerDataService;
    private final IPlayerSessionStore sessionStore;
    private final IUiNavigator uiNavigator;
    private final Path requestPath;
    private final Path ackPath;
    private volatile String lastRequestId;
    private ScheduledFuture<?> pollTask;

    public VisualVerificationControlService(
            IPlayerDataService playerDataService,
            IPlayerSessionStore sessionStore,
            IUiNavigator uiNavigator
    ) {
        this.playerDataService = Objects.requireNonNull(playerDataService, "playerDataService");
        this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore");
        this.uiNavigator = Objects.requireNonNull(uiNavigator, "uiNavigator");
        Path controlRoot = Path.of(System.getProperty("user.dir", "."), "visual-control");
        this.requestPath = controlRoot.resolve("resource-game-ui-request.properties");
        this.ackPath = controlRoot.resolve("resource-game-ui-request.ack.properties");
    }

    public void start() {
        if (pollTask != null) {
            return;
        }
        pollTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(
                this::pollRequestFile,
                POLL_INTERVAL_MILLIS,
                POLL_INTERVAL_MILLIS,
                TimeUnit.MILLISECONDS
        );
    }

    public void shutdown() {
        if (pollTask != null) {
            pollTask.cancel(false);
            pollTask = null;
        }
    }

    private void pollRequestFile() {
        try {
            if (!Files.isRegularFile(requestPath)) {
                return;
            }
            Properties request = readProperties(requestPath);
            String requestId = request.getProperty("requestId", "").trim();
            if (requestId.isEmpty() || requestId.equals(lastRequestId)) {
                return;
            }
            lastRequestId = requestId;
            handleRequest(requestId, request);
        } catch (Throwable throwable) {
            Throwable rootCause = rootCause(throwable);
            LOGGER.at(Level.WARNING).withCause(rootCause).log(
                    "Failed to process visual verification request file. cause=%s: %s",
                    rootCause.getClass().getName(),
                    safeMessage(rootCause)
            );
        }
    }

    private void handleRequest(String requestId, Properties request) {
        String requestedUi = request.getProperty("ui", "debug");
        boolean closeRequest = isCloseRequest(requestedUi);
        UiPageType pageType = closeRequest ? null : parseUiPage(requestedUi);
        PlayerRef playerRef = resolvePlayerRef(request.getProperty("player", "*"));
        if (!closeRequest && pageType == null) {
            writeAck(requestId, "error", "Unknown UI page: " + requestedUi, null);
            return;
        }
        if (playerRef == null) {
            writeAck(requestId, "error", "No matching online player.", null);
            return;
        }
        World world = resolveWorld(playerRef);
        if (world == null) {
            writeAck(requestId, "error", "Player world is not ready.", null);
            return;
        }
        WorldTasks.executeSafe(
                world,
                "VisualVerificationControlService.handleRequestOnWorldThread",
                () -> {
                    if (closeRequest) {
                        closeUiOnWorldThread(requestId, playerRef, world);
                        return;
                    }
                    handleRequestOnWorldThread(requestId, pageType, playerRef, world);
                }
        );
    }

    private boolean isCloseRequest(String requestedUi) {
        if (requestedUi == null) {
            return false;
        }
        String normalized = requestedUi.toLowerCase(Locale.ROOT).replace("-", "_").replace(" ", "_");
        return "close".equals(normalized) || "none".equals(normalized) || "page_none".equals(normalized);
    }

    private void handleRequestOnWorldThread(String requestId, UiPageType pageType, PlayerRef playerRef, World expectedWorld) {
        World currentWorld = resolveWorld(playerRef);
        if (currentWorld == null) {
            writeAck(requestId, "error", "Player world is not ready.", null);
            return;
        }
        if (currentWorld != expectedWorld) {
            WorldTasks.executeSafe(
                    currentWorld,
                    "VisualVerificationControlService.handleRequestOnCurrentWorld",
                    () -> handleRequestOnWorldThread(requestId, pageType, playerRef, currentWorld)
            );
            return;
        }
        Player player = resolvePlayerOnWorldThread(playerRef);
        if (player == null) {
            writeAck(requestId, "error", "Live player component is not ready.", null);
            return;
        }
        playerDataService.ensureSession(player).whenComplete((ignored, throwable) -> {
            if (throwable != null) {
                writeAck(requestId, "error", safeMessage(rootCause(throwable)), null);
                return;
            }
            openUiOnPlayerWorld(requestId, pageType, playerRef);
        });
    }

    private void openUiOnPlayerWorld(String requestId, UiPageType pageType, PlayerRef playerRef) {
        World world = resolveWorld(playerRef);
        if (world == null) {
            writeAck(requestId, "error", "Player world is not ready.", null);
            return;
        }
        WorldTasks.executeSafe(world, "VisualVerificationControlService.openUi", () -> {
            Player player = resolvePlayerOnWorldThread(playerRef);
            if (player == null) {
                writeAck(requestId, "error", "Live player component is not ready.", null);
                return;
            }
            openUi(requestId, player, pageType);
        });
    }

    private void closeUiOnWorldThread(String requestId, PlayerRef playerRef, World expectedWorld) {
        World currentWorld = resolveWorld(playerRef);
        if (currentWorld == null) {
            writeAck(requestId, "error", "Player world is not ready.", null);
            return;
        }
        if (currentWorld != expectedWorld) {
            WorldTasks.executeSafe(
                    currentWorld,
                    "VisualVerificationControlService.closeUiOnCurrentWorld",
                    () -> closeUiOnWorldThread(requestId, playerRef, currentWorld)
            );
            return;
        }
        Player player = resolvePlayerOnWorldThread(playerRef);
        if (player == null || player.getPlayerRef() == null) {
            writeAck(requestId, "error", "Live player component is not ready.", null);
            return;
        }
        Ref<EntityStore> ref = player.getPlayerRef().getReference();
        if (ref == null || !ref.isValid()) {
            writeAck(requestId, "error", "Live player reference is not ready.", null);
            return;
        }
        player.getPageManager().setPage(ref, ref.getStore(), Page.None);
        uiNavigator.clearTrackedPage(player.getUuid());
        writeAck(requestId, "closed", "Page.None", player);
    }

    private void openUi(String requestId, Player player, UiPageType pageType) {
        PlayerSession session = sessionStore.get(player.getUuid());
        if (session == null) {
            writeAck(requestId, "error", "Player session is not ready.", player);
            return;
        }
        uiNavigator.open(pageType, player, new UiNavigationContext(player.getUuid(), player.getDisplayName()), session.gameState());
        writeAck(requestId, "opened", pageType.name(), player);
    }

    private PlayerRef resolvePlayerRef(String token) {
        String normalized = token == null ? "*" : token.trim();
        for (PlayerRef playerRef : Universe.get().getPlayers()) {
            if (playerRef == null || !playerRef.isValid() || resolveWorld(playerRef) == null) {
                continue;
            }
            if (normalized.isBlank() || "*".equals(normalized)) {
                return playerRef;
            }
            String username = playerRef.getUsername();
            if (username != null && username.equalsIgnoreCase(normalized)) {
                return playerRef;
            }
            UUID playerId = playerRef.getUuid();
            if (playerId != null && playerId.toString().startsWith(normalized)) {
                return playerRef;
            }
        }
        return null;
    }

    private Player resolvePlayerOnWorldThread(PlayerRef playerRef) {
        if (playerRef == null) {
            return null;
        }
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return null;
        }
        return ref.getStore().getComponent(ref, Player.getComponentType());
    }

    private World resolveWorld(PlayerRef playerRef) {
        if (playerRef == null || playerRef.getWorldUuid() == null) {
            return null;
        }
        return Universe.get().getWorld(playerRef.getWorldUuid());
    }

    private UiPageType parseUiPage(String token) {
        String normalized = token == null ? "debug" : token.toLowerCase(Locale.ROOT).replace("-", "_").replace(" ", "_");
        return switch (normalized) {
            case "castle", "main", "castle_main" -> UiPageType.CASTLE_MAIN;
            case "info", "castle_info" -> UiPageType.CASTLE_INFO;
            case "citizens", "castle_citizens" -> UiPageType.CASTLE_CITIZENS;
            case "troops", "castle_troops" -> UiPageType.CASTLE_TROOPS;
            case "resources", "castle_resources" -> UiPageType.CASTLE_RESOURCES;
            case "upgrades", "castle_upgrades" -> UiPageType.CASTLE_UPGRADES;
            case "buildings", "building", "castle_buildings" -> UiPageType.CASTLE_BUILDINGS;
            case "building_detail", "buildingdetail" -> UiPageType.BUILDING_DETAIL;
            case "interior", "interior_main" -> UiPageType.INTERIOR_MAIN;
            case "debug", "debug_navigator" -> UiPageType.DEBUG_NAVIGATOR;
            default -> null;
        };
    }

    private Properties readProperties(Path path) throws IOException {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(path)) {
            properties.load(reader);
        }
        Properties normalized = new Properties();
        for (String name : properties.stringPropertyNames()) {
            normalized.setProperty(stripBom(name), properties.getProperty(name));
        }
        return normalized;
    }

    private String stripBom(String value) {
        if (value == null || value.isEmpty() || value.charAt(0) != '\uFEFF') {
            return value;
        }
        return value.substring(1);
    }

    private void writeAck(String requestId, String status, String message, Player player) {
        try {
            Files.createDirectories(ackPath.getParent());
            Properties ack = new Properties();
            ack.setProperty("requestId", requestId);
            ack.setProperty("status", status);
            ack.setProperty("message", message == null ? "" : message);
            ack.setProperty("writtenAt", Instant.now().toString());
            if (player != null) {
                ack.setProperty("playerName", player.getDisplayName() == null ? "" : player.getDisplayName());
                ack.setProperty("playerUuid", player.getUuid() == null ? "" : player.getUuid().toString());
            }
            try (Writer writer = Files.newBufferedWriter(ackPath)) {
                ack.store(writer, "Resource game visual verification ack");
            }
        } catch (IOException exception) {
            LOGGER.at(Level.WARNING).withCause(exception).log("Failed to write visual verification ack file.");
        }
    }

    private String safeMessage(Throwable throwable) {
        if (throwable == null || throwable.getMessage() == null || throwable.getMessage().isBlank()) {
            return "unexpected error";
        }
        return throwable.getMessage();
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current != null && current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current == null ? new RuntimeException("unknown") : current;
    }
}
