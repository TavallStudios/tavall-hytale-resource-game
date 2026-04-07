package org.tavall.hytale.resourcegame.clock;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public record KingdomClockState(
    Instant observedAt,
    ZoneId zoneId,
    int hour,
    DayPhase dayPhase,
    boolean daylight
) {

  public static KingdomClockState from(Instant instant, ZoneId zoneId) {
    ZonedDateTime zoneTime = instant.atZone(zoneId);
    int hour = zoneTime.getHour();
    DayPhase phase = hour >= 6 && hour < 18 ? DayPhase.DAY : DayPhase.NIGHT;
    return new KingdomClockState(instant, zoneId, hour, phase, phase == DayPhase.DAY);
  }
}
