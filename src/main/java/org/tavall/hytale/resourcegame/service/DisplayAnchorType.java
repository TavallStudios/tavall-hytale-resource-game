package org.tavall.hytale.resourcegame.service;

import java.util.UUID;

public enum DisplayAnchorType {
  CITIZEN,
  TROOP;

  public String labelPrefix() {
    return this == CITIZEN ? "Citizens" : "Troops";
  }

  public UUID selectAnchor(InteriorDisplayState state) {
    return this == CITIZEN ? state.citizenAnchorEntityId() : state.troopAnchorEntityId();
  }
}
