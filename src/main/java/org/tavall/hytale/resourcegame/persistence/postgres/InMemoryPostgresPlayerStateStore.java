package org.tavall.hytale.resourcegame.persistence.postgres;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.tavall.hytale.resourcegame.domain.castle.CastleLocation;
import org.tavall.hytale.resourcegame.domain.player.PlayerGameState;
import org.tavall.hytale.resourcegame.domain.player.PlayerProfile;
import org.tavall.hytale.resourcegame.domain.player.PlayerStateBundle;
import org.tavall.hytale.resourcegame.domain.player.PopulationAgingProfile;
import org.tavall.hytale.resourcegame.domain.population.CitizenAttributes;
import org.tavall.hytale.resourcegame.domain.population.CitizenJob;
import org.tavall.hytale.resourcegame.domain.population.CitizenUnitProfile;
import org.tavall.hytale.resourcegame.domain.population.PopulationRoster;
import org.tavall.hytale.resourcegame.domain.population.PopulationSummary;
import org.tavall.hytale.resourcegame.domain.resource.ResourceInventory;
import org.tavall.hytale.resourcegame.domain.resource.ResourceType;
import org.tavall.hytale.resourcegame.persistence.PlayerStateStore;

/**
 * In-memory substitute for Postgres persistence used in local harness tests.
 */
public class InMemoryPostgresPlayerStateStore implements PlayerStateStore {

  private final AtomicLong idSequence = new AtomicLong(1);
  private final ConcurrentHashMap<UUID, PlayerStateBundle> bundlesByPlayerId = new ConcurrentHashMap<>();

  @Override
  public Optional<PlayerStateBundle> load(UUID playerId) {
    PlayerStateBundle bundle = bundlesByPlayerId.get(playerId);
    if (bundle == null) {
      return Optional.empty();
    }
    return Optional.of(copy(bundle));
  }

  @Override
  public PlayerStateBundle save(PlayerStateBundle bundle) {
    if (bundle.profile().internalPlayerId() <= 0) {
      long nextId = idSequence.getAndIncrement();
      bundle.profile().assignInternalPlayerId(nextId);
      bundle.gameState().assignProfileId(nextId);
    }
    bundlesByPlayerId.put(bundle.playerId(), copy(bundle));
    return bundle;
  }

  private PlayerStateBundle copy(PlayerStateBundle source) {
    PlayerProfile profile = new PlayerProfile(
        source.profile().internalPlayerId(),
        source.profile().playerUuid(),
        source.profile().playerName(),
        source.profile().timezone(),
        source.profile().transformedIp(),
        source.profile().createdAt(),
        source.profile().updatedAt(),
        source.profile().lastSeenAt()
    );

    ResourceInventory resources = new ResourceInventory();
    resources.set(ResourceType.FOOD, source.gameState().resources().get(ResourceType.FOOD));
    resources.set(ResourceType.WOOD, source.gameState().resources().get(ResourceType.WOOD));
    resources.set(ResourceType.IRON, source.gameState().resources().get(ResourceType.IRON));

    CastleLocation location = source.gameState().castleLocation();
    CastleLocation copiedLocation = location == null
        ? null
        : new CastleLocation(location.worldId(), location.x(), location.y(), location.z());

    PopulationAgingProfile agingProfile = new PopulationAgingProfile(
        source.gameState().agingProfile().unresolvedCadence(),
        source.gameState().agingProfile().lastAgingEvaluation()
    );

    PlayerGameState gameState = new PlayerGameState(
        source.gameState().profileId(),
        source.gameState().castleId(),
        copiedLocation,
        source.gameState().citizenCount(),
        source.gameState().troopCount(),
        resources,
        source.gameState().currentInteriorWorldId(),
        source.gameState().createdAt(),
        source.gameState().updatedAt(),
        agingProfile,
        source.gameState().populationMetadataJson()
    );

    PopulationRoster roster = new PopulationRoster();
    for (CitizenUnitProfile unit : source.populationRoster().allUnits()) {
      CitizenUnitProfile clone = new CitizenUnitProfile(
          unit.unitId(),
          unit.role(),
          unit.job(),
          new CitizenAttributes(
              unit.attributes().strength(),
              unit.attributes().discipline(),
              unit.attributes().craft(),
              unit.attributes().morale()
          ),
          unit.birthAt(),
          unit.lastRoleShiftAt()
      );
      roster.addUnit(clone);
    }
    if (roster.allUnits().isEmpty()) {
      roster.syncToTarget(new PopulationSummary(gameState.citizenCount(), gameState.troopCount()), Instant.now());
      for (CitizenUnitProfile generated : roster.allUnits()) {
        generated.assignJob(generated.role() == org.tavall.hytale.resourcegame.domain.population.PopulationRole.TROOP
            ? CitizenJob.SOLDIER
            : CitizenJob.IDLE);
      }
    }

    return new PlayerStateBundle(source.playerId(), profile, gameState, roster);
  }
}
