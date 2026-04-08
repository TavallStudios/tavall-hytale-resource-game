package com.tavall.hytale.resourcegame.services;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class InteriorInstanceServiceTest {
    @Test
    void worldNameForUsesStableDedicatedPrefix() {
        InteriorInstanceService service = new InteriorInstanceService();

        String worldName = service.worldNameFor(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));

        assertEquals("kingdom-interior-123e4567e89b12d3a456426614174000", worldName);
    }
}
