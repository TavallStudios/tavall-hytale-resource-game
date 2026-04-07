package org.tavall.hytale.resourcegame.service.model;

import java.util.UUID;
import org.tavall.hytale.resourcegame.domain.castle.CastleRecord;

public record CastleInteractionContext(UUID playerId, CastleRecord castleRecord, UUID worldEntityId) {
}
