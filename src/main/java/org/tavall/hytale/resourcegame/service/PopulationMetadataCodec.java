package org.tavall.hytale.resourcegame.service;

import org.tavall.hytale.resourcegame.domain.population.CitizenUnitProfile;
import org.tavall.hytale.resourcegame.domain.population.PopulationRoster;

public class PopulationMetadataCodec {

  public String encode(PopulationRoster roster) {
    StringBuilder builder = new StringBuilder();
    builder.append("units=").append(roster.allUnits().size());
    for (CitizenUnitProfile unit : roster.allUnits()) {
      builder.append("|")
          .append(unit.unitId())
          .append(",")
          .append(unit.role().name())
          .append(",")
          .append(unit.job().name())
          .append(",")
          .append(unit.attributes().strength())
          .append(":")
          .append(unit.attributes().discipline())
          .append(":")
          .append(unit.attributes().craft())
          .append(":")
          .append(unit.attributes().morale());
    }
    return builder.toString();
  }
}
