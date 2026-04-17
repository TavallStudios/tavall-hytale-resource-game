package com.tavall.hytale.resourcegame.services;

import com.tavall.hytale.resourcegame.domain.FocusedWorldTargetType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class FocusedWorldOverrideServiceTest {
    @Test
    void peekAndConsumeNodeOverrideReturnSelectedTarget() {
        FocusedWorldOverrideService service = new FocusedWorldOverrideService();
        UUID playerId = UUID.fromString("11111111-2222-3333-4444-555555555555");
        UUID nodeId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

        service.markNode(playerId, nodeId);

        var peeked = service.peek(playerId);
        assertTrue(peeked.isPresent());
        assertEquals(FocusedWorldTargetType.RESOURCE_NODE, peeked.get().type());
        assertEquals(nodeId, peeked.get().nodeId());

        var consumed = service.consume(playerId);
        assertTrue(consumed.isPresent());
        assertEquals(nodeId, consumed.get().nodeId());
        assertTrue(service.peek(playerId).isEmpty());
    }

    @Test
    void castleOverrideCanBeClearedExplicitly() {
        FocusedWorldOverrideService service = new FocusedWorldOverrideService();
        UUID playerId = UUID.fromString("11111111-2222-3333-4444-555555555555");

        service.markCastle(playerId);
        assertTrue(service.peek(playerId).isPresent());

        service.clear(playerId);
        assertTrue(service.consume(playerId).isEmpty());
    }
}
