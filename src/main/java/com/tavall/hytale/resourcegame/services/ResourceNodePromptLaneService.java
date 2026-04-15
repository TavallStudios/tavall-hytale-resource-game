package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerTeleportService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IResourceNodePromptLaneService;
import com.tavall.hytale.resourcegame.domain.ResourceNodeData;
import com.tavall.hytale.resourcegame.world.ResourceNodePromptLaneLayout;
import com.tavall.hytale.resourcegame.world.ResourceNodePromptLaneLayoutService;
import com.tavall.hytale.resourcegame.world.ResourceNodePromptLaneStructureService;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Aligns players onto a safe in-world lane for resource-node prompt interaction.
 */
public final class ResourceNodePromptLaneService implements IResourceNodePromptLaneService, IDependencyInjectableConcrete {
    private static final Logger LOGGER = Logger.getLogger(ResourceNodePromptLaneService.class.getName());
    private static final double ALIGNMENT_EPSILON = 0.25D;
    private static final long TELEPORT_DELAY_MILLIS = 750L;

    private final ResourceNodePromptLaneLayoutService layoutService;
    private final ResourceNodePromptLaneStructureService structureService;
    private final IPlayerTeleportService playerTeleportService;

    public ResourceNodePromptLaneService(
            ResourceNodePromptLaneLayoutService layoutService,
            ResourceNodePromptLaneStructureService structureService,
            IPlayerTeleportService playerTeleportService
    ) {
        this.layoutService = Objects.requireNonNull(layoutService, "layoutService");
        this.structureService = Objects.requireNonNull(structureService, "structureService");
        this.playerTeleportService = Objects.requireNonNull(playerTeleportService, "playerTeleportService");
    }

    @Override
    public void alignPlayer(Player player, ResourceNodeData node) {
        ResourceNodePromptLaneLayout layout = layoutService.createLayout(node);
        Vector3d lookTarget = node.location().toVector();
        player.getWorld().execute(() -> {
            structureService.ensurePromptLane(player.getWorld(), layout);
            Vector3d alignmentPosition = playerTeleportService.standingPosition(player, layout.alignmentPoint());
            LOGGER.info(() -> "Node prompt lane align for " + player.getUuid()
                    + " node=" + node.nodeId()
                    + " origin=" + layout.origin()
                    + " alignmentBase=" + layout.alignmentPoint()
                    + " teleport=" + alignmentPosition);
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
        double dy = currentPosition.getY() - alignmentPosition.getY();
        double dz = currentPosition.getZ() - alignmentPosition.getZ();
        return Math.sqrt((dx * dx) + (dy * dy) + (dz * dz)) <= ALIGNMENT_EPSILON;
    }
}
