package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.tavall.hytale.resourcegame.domain.CastleLocationData;
import com.tavall.hytale.resourcegame.world.CastlePromptLaneLayout;
import com.tavall.hytale.resourcegame.world.CastlePromptLaneLayoutService;
import com.tavall.hytale.resourcegame.world.CastlePromptLaneStructureService;

import java.util.Objects;

/**
 * Aligns players onto a safe in-world lane for castle prompt interaction.
 */
public final class CastlePromptLaneService {
    private static final long TELEPORT_DELAY_MILLIS = 750L;

    private final CastlePromptLaneLayoutService layoutService;
    private final CastlePromptLaneStructureService structureService;
    private final PlayerTeleportService playerTeleportService;

    public CastlePromptLaneService(
            CastlePromptLaneLayoutService layoutService,
            CastlePromptLaneStructureService structureService,
            PlayerTeleportService playerTeleportService
    ) {
        this.layoutService = Objects.requireNonNull(layoutService, "layoutService");
        this.structureService = Objects.requireNonNull(structureService, "structureService");
        this.playerTeleportService = Objects.requireNonNull(playerTeleportService, "playerTeleportService");
    }

    public void alignPlayer(Player player, CastleLocationData castleLocation) {
        CastlePromptLaneLayout layout = layoutService.createLayout(castleLocation);
        player.getWorld().execute(() -> {
            structureService.ensurePromptLane(player.getWorld(), layout);
            Vector3d alignmentPosition = playerTeleportService.standingPosition(player, layout.alignmentPoint());
            playerTeleportService.teleportAfterDelay(player, alignmentPosition, TELEPORT_DELAY_MILLIS);
        });
    }
}
