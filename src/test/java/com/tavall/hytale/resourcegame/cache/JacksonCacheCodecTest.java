package com.tavall.hytale.resourcegame.cache;

import com.tavall.hytale.resourcegame.domain.AgingState;
import com.tavall.hytale.resourcegame.domain.CastleLocationData;
import com.tavall.hytale.resourcegame.domain.CitizenJobType;
import com.tavall.hytale.resourcegame.domain.CitizenMetaData;
import com.tavall.hytale.resourcegame.domain.InteriorSessionData;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.domain.PlayerProfile;
import com.tavall.hytale.resourcegame.domain.PopulationSummary;
import com.tavall.hytale.resourcegame.domain.ResourceInventory;
import com.tavall.hytale.resourcegame.domain.TroopMetaData;
import com.tavall.hytale.resourcegame.services.JsonMapperProvider;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class JacksonCacheCodecTest {

    @Test
    void roundTripsPlayerProfile() {
        Instant now = Instant.parse("2026-04-08T11:47:54Z");
        PlayerProfile profile = new PlayerProfile(
                42L,
                UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
                "PersistenceBot",
                "UTC",
                "hashed-ip",
                now,
                now,
                now
        );

        JacksonCacheCodec<PlayerProfile> codec = new JacksonCacheCodec<>(
                new JsonMapperProvider().mapper(),
                PlayerProfile.class,
                "player-profile-test"
        );

        PlayerProfile decoded = codec.decode(codec.encode(profile));

        assertEquals(profile.id(), decoded.id());
        assertEquals(profile.uuid(), decoded.uuid());
        assertEquals(profile.name(), decoded.name());
        assertEquals(profile.timezone(), decoded.timezone());
        assertEquals(profile.ipHash(), decoded.ipHash());
        assertEquals(profile.createdAt(), decoded.createdAt());
        assertEquals(profile.updatedAt(), decoded.updatedAt());
        assertEquals(profile.lastSeenAt(), decoded.lastSeenAt());
    }

    @Test
    void roundTripsPlayerGameState() {
        Instant now = Instant.parse("2026-04-08T11:47:54Z");
        CastleLocationData castleLocation = new CastleLocationData("default", 89.5, 120.1, 167.5);
        PopulationSummary populationSummary = new PopulationSummary(
                17,
                3,
                new CitizenMetaData(0.7, 0.4, 0.8, Map.of(CitizenJobType.GATHERER, 9, CitizenJobType.SOLDIER, 3)),
                new TroopMetaData(0.6, 0.7, 0.8),
                new AgingState(now, Duration.ofHours(6))
        );
        PlayerGameState state = new PlayerGameState(
                7L,
                42L,
                UUID.fromString("79c227d1-40f5-4b88-9f64-bc9b02d766ea"),
                "stone_column_castle",
                castleLocation,
                populationSummary,
                new ResourceInventory(61, 73, 29),
                new InteriorSessionData("kingdom-interior-123e4567e89b12d3a456426614174000", castleLocation, now),
                "{\"note\":\"round-trip\"}",
                now,
                now
        );

        JacksonCacheCodec<PlayerGameState> codec = new JacksonCacheCodec<>(
                new JsonMapperProvider().mapper(),
                PlayerGameState.class,
                "player-game-state-test"
        );

        PlayerGameState decoded = codec.decode(codec.encode(state));

        assertEquals(state.id(), decoded.id());
        assertEquals(state.profileId(), decoded.profileId());
        assertEquals(state.castleId(), decoded.castleId());
        assertEquals(state.castleAssetType(), decoded.castleAssetType());
        assertEquals(state.castleLocation().worldName(), decoded.castleLocation().worldName());
        assertEquals(state.castleLocation().x(), decoded.castleLocation().x());
        assertEquals(state.castleLocation().y(), decoded.castleLocation().y());
        assertEquals(state.castleLocation().z(), decoded.castleLocation().z());
        assertEquals(state.populationSummary().citizenCount(), decoded.populationSummary().citizenCount());
        assertEquals(state.populationSummary().troopCount(), decoded.populationSummary().troopCount());
        assertEquals(state.populationSummary().citizenMetaData().jobCounts(), decoded.populationSummary().citizenMetaData().jobCounts());
        assertEquals(state.populationSummary().agingState().lastTickAt(), decoded.populationSummary().agingState().lastTickAt());
        assertEquals(state.populationSummary().agingState().totalAge(), decoded.populationSummary().agingState().totalAge());
        assertEquals(state.resources().food(), decoded.resources().food());
        assertEquals(state.resources().wood(), decoded.resources().wood());
        assertEquals(state.resources().iron(), decoded.resources().iron());
        assertEquals(state.interiorSession().interiorWorldName(), decoded.interiorSession().interiorWorldName());
        assertEquals(state.metadataJson(), decoded.metadataJson());
        assertEquals(state.createdAt(), decoded.createdAt());
        assertEquals(state.updatedAt(), decoded.updatedAt());
    }
}
