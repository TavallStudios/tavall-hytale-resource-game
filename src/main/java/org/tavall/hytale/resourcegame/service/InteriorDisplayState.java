package org.tavall.hytale.resourcegame.service;

import java.util.Set;
import java.util.UUID;

public record InteriorDisplayState(
    UUID citizenAnchorEntityId,
    UUID troopAnchorEntityId,
    Set<UUID> citizenCopies,
    Set<UUID> troopCopies
) {
}
