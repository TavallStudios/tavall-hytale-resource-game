package org.tavall.hytale.resourcegame.domain.population;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Active unit roster kept in-memory for high-frequency simulation updates.
 */
public class PopulationRoster {

  private final LinkedHashMap<UUID, CitizenUnitProfile> unitsById = new LinkedHashMap<>();

  public List<CitizenUnitProfile> allUnits() {
    return List.copyOf(unitsById.values());
  }

  public void addUnit(CitizenUnitProfile unit) {
    unitsById.put(unit.unitId(), unit);
  }

  public void replaceWith(Iterable<CitizenUnitProfile> units) {
    unitsById.clear();
    for (CitizenUnitProfile unit : units) {
      addUnit(unit);
    }
  }

  public PopulationSummary summary() {
    int citizens = 0;
    int troops = 0;
    for (CitizenUnitProfile unit : unitsById.values()) {
      if (unit.role() == PopulationRole.CITIZEN) {
        citizens++;
      } else {
        troops++;
      }
    }
    return new PopulationSummary(citizens, troops);
  }

  public void syncToTarget(PopulationSummary target, Instant now) {
    PopulationSummary current = summary();
    if (target.total() > current.total()) {
      int additions = target.total() - current.total();
      appendUnits(additions, now);
    }
    if (target.total() < current.total()) {
      trimUnits(current.total() - target.total());
    }
    alignRoleCounts(target.troops(), now);
  }

  public Map<UUID, CitizenUnitProfile> snapshotById() {
    return Map.copyOf(unitsById);
  }

  private void appendUnits(int additions, Instant now) {
    for (int index = 0; index < additions; index++) {
      CitizenAttributes attributes = randomAttributes();
      CitizenUnitProfile unit = new CitizenUnitProfile(
          UUID.randomUUID(),
          PopulationRole.CITIZEN,
          CitizenJob.IDLE,
          attributes,
          now,
          now
      );
      unitsById.put(unit.unitId(), unit);
    }
  }

  private void trimUnits(int removals) {
    List<UUID> keys = new ArrayList<>(unitsById.keySet());
    for (int index = 0; index < removals && index < keys.size(); index++) {
      unitsById.remove(keys.get(keys.size() - 1 - index));
    }
  }

  private void alignRoleCounts(int targetTroops, Instant now) {
    List<CitizenUnitProfile> citizens = new ArrayList<>();
    List<CitizenUnitProfile> troops = new ArrayList<>();
    for (CitizenUnitProfile unit : unitsById.values()) {
      if (unit.role() == PopulationRole.CITIZEN) {
        citizens.add(unit);
      } else {
        troops.add(unit);
      }
    }
    while (troops.size() < targetTroops && !citizens.isEmpty()) {
      CitizenUnitProfile next = citizens.remove(citizens.size() - 1);
      next.assignRole(PopulationRole.TROOP, now);
      troops.add(next);
    }
    while (troops.size() > targetTroops && !troops.isEmpty()) {
      CitizenUnitProfile next = troops.remove(troops.size() - 1);
      next.assignRole(PopulationRole.CITIZEN, now);
      citizens.add(next);
    }
  }

  private CitizenAttributes randomAttributes() {
    ThreadLocalRandom random = ThreadLocalRandom.current();
    return new CitizenAttributes(
        random.nextInt(20, 81),
        random.nextInt(20, 81),
        random.nextInt(20, 81),
        random.nextInt(20, 81)
    );
  }
}
