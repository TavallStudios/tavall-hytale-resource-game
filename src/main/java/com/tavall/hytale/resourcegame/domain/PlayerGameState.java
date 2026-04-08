package com.tavall.hytale.resourcegame.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Persistent game state for a player.
 */
public final class PlayerGameState {
    private final long id;
    private final long profileId;
    private final UUID castleId;
    private final CastleLocationData castleLocation;
    private final PopulationSummary populationSummary;
    private final ResourceInventory resources;
    private final InteriorSessionData interiorSession;
    private final String metadataJson;
    private final Instant createdAt;
    private final Instant updatedAt;

    public PlayerGameState(
            long id,
            long profileId,
            UUID castleId,
            CastleLocationData castleLocation,
            PopulationSummary populationSummary,
            ResourceInventory resources,
            InteriorSessionData interiorSession,
            String metadataJson,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.profileId = profileId;
        this.castleId = castleId;
        this.castleLocation = castleLocation;
        this.populationSummary = Objects.requireNonNull(populationSummary, "populationSummary");
        this.resources = Objects.requireNonNull(resources, "resources");
        this.interiorSession = interiorSession;
        this.metadataJson = metadataJson;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public long id() {
        return id;
    }

    public long profileId() {
        return profileId;
    }

    public UUID castleId() {
        return castleId;
    }

    public CastleLocationData castleLocation() {
        return castleLocation;
    }

    public PopulationSummary populationSummary() {
        return populationSummary;
    }

    public ResourceInventory resources() {
        return resources;
    }

    public InteriorSessionData interiorSession() {
        return interiorSession;
    }

    public String metadataJson() {
        return metadataJson;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public PlayerGameState withResources(ResourceInventory resources, Instant updatedAt) {
        return new PlayerGameState(
                id,
                profileId,
                castleId,
                castleLocation,
                populationSummary,
                resources,
                interiorSession,
                metadataJson,
                createdAt,
                updatedAt
        );
    }

    public PlayerGameState withPopulation(PopulationSummary populationSummary, Instant updatedAt) {
        return new PlayerGameState(
                id,
                profileId,
                castleId,
                castleLocation,
                populationSummary,
                resources,
                interiorSession,
                metadataJson,
                createdAt,
                updatedAt
        );
    }

    public PlayerGameState withCastleLocation(CastleLocationData castleLocation, UUID castleId, Instant updatedAt) {
        return new PlayerGameState(
                id,
                profileId,
                castleId,
                castleLocation,
                populationSummary,
                resources,
                interiorSession,
                metadataJson,
                createdAt,
                updatedAt
        );
    }

    public PlayerGameState withInteriorSession(InteriorSessionData interiorSession, Instant updatedAt) {
        return new PlayerGameState(
                id,
                profileId,
                castleId,
                castleLocation,
                populationSummary,
                resources,
                interiorSession,
                metadataJson,
                createdAt,
                updatedAt
        );
    }

    public PlayerGameState withMetadataJson(String metadataJson, Instant updatedAt) {
        return new PlayerGameState(
                id,
                profileId,
                castleId,
                castleLocation,
                populationSummary,
                resources,
                interiorSession,
                metadataJson,
                createdAt,
                updatedAt
        );
    }
}
