package org.tavall.hytale.resourcegame.domain.player;

import java.time.ZoneId;
import java.util.UUID;
import org.tavall.hytale.resourcegame.runtime.WorldPosition;

/**
 * Player join payload normalized before hydration and first-init orchestration.
 */
public record PlayerJoinRequest(
    UUID playerId,
    String playerName,
    ZoneId timezone,
    String transformedIp,
    WorldPosition joinPosition
) {
}
