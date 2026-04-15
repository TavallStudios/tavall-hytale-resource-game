package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastlePromptLaneService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerTeleportService;
import com.tavall.hytale.resourcegame.domain.CastleLocationData;
import com.tavall.hytale.resourcegame.world.CastlePromptLaneLayout;
import com.tavall.hytale.resourcegame.world.CastlePromptLaneLayoutService;
import com.tavall.hytale.resourcegame.world.CastlePromptLaneStructureService;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Aligns players onto a safe in-world lane for castle prompt interaction.
 */
public final class CastlePromptLaneService implements ICastlePromptLaneService, IDependencyInjectableConcrete {
    private static final double ALIGNMENT_EPSILON = 0.25D;
    private static final long TELEPORT_DELAY_MILLIS = 750L;

    private final CastlePromptLaneLayoutService layoutService;
    private final CastlePromptLaneStructureService structureService;
    private final IPlayerTeleportService playerTeleportService;

    public CastlePromptLaneService(
            CastlePromptLaneLayoutService layoutService,
            CastlePromptLaneStructureService structureService,
            IPlayerTeleportService playerTeleportService
    ) {
        this.layoutService = Objects.requireNonNull(layoutService, "layoutService");
        this.structureService = Objects.requireNonNull(structureService, "structureService");
        this.playerTeleportService = Objects.requireNonNull(playerTeleportService, "playerTeleportService");
    }

    public void alignPlayer(Player player, CastleLocationData castleLocation) {
        CastlePromptLaneLayout layout = layoutService.createLayout(castleLocation);
        Vector3d lookTarget = castleLocation.toVector();
        player.getWorld().execute(() -> {
            structureService.ensurePromptLane(player.getWorld(), layout);
            Vector3d alignmentPosition = playerTeleportService.standingPosition(player, layout.alignmentPoint());
            if (alreadyAligned(player, alignmentPosition)) {
                playerTeleportService.orientPlayer(player, lookTarget);
                return;
            }
            playerTeleportService.teleportAfterDelay(player, alignmentPosition, TELEPORT_DELAY_MILLIS);
            HytaleServer.SCHEDULED_EXECUTOR.schedule(
                    () -> playerTeleportService.orientPlayer(player, lookTarget),
                    TELEPORT_DELAY_MILLIS + 150L,
                    TimeUnit.MILLISECONDS
            );
        });
    }

    private boolean alreadyAligned(Player player, Vector3d alignmentPosition) {
        TransformComponent transform = player.getTransformComponent();
        if (transform == null || alignmentPosition == null) {
            return false;
        }
        Vector3d currentPosition = transform.getPosition();
        if (currentPosition == null) {
            return false;
        }
        double dx = currentPosition.getX() - alignmentPosition.getX();
        double dz = currentPosition.getZ() - alignmentPosition.getZ();
        return Math.sqrt((dx * dx) + (dz * dz)) <= ALIGNMENT_EPSILON;
    }
}
