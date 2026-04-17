package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastlePromptLaneService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerTeleportService;
import com.tavall.hytale.resourcegame.domain.CastleLocationData;
import com.tavall.hytale.resourcegame.world.CastlePromptLaneLayout;
import com.tavall.hytale.resourcegame.world.CastlePromptLaneLayoutService;
import com.tavall.hytale.resourcegame.world.CastlePromptLaneStructureService;

import java.util.Objects;

/**
 * Prepares the in-world prompt lane and orients players for castle prompt interaction.
 */
public final class CastlePromptLaneService implements ICastlePromptLaneService, IDependencyInjectableConcrete {
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
            playerTeleportService.orientPlayer(player, lookTarget);
        });
    }
}
