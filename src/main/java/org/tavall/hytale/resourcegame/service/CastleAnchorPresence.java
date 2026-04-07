package org.tavall.hytale.resourcegame.service;

import java.util.UUID;
import org.tavall.hytale.resourcegame.domain.castle.CastleRecord;

public record CastleAnchorPresence(CastleRecord castleRecord, UUID worldEntityId) {
}
