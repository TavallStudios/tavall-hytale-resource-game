package org.tavall.hytale.resourcegame.service.model;

import java.util.UUID;
import org.tavall.hytale.resourcegame.domain.resource.ResourceType;

public record DebugMutationRequest(
    UUID playerId,
    int amount,
    ResourceType resourceType,
    String reason
) {
}
