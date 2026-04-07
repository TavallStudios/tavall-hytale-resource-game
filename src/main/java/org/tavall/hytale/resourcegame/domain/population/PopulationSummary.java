package org.tavall.hytale.resourcegame.domain.population;

public record PopulationSummary(int citizens, int troops) {

  public PopulationSummary {
    if (citizens < 0 || troops < 0) {
      throw new IllegalArgumentException("population counts cannot be negative");
    }
  }

  public int total() {
    return citizens + troops;
  }
}
