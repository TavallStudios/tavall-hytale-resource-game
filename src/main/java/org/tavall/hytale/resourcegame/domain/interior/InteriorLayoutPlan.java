package org.tavall.hytale.resourcegame.domain.interior;

import org.tavall.hytale.resourcegame.runtime.WorldPosition;

public record InteriorLayoutPlan(
    WorldPosition citizenAnchorPosition,
    WorldPosition troopAnchorPosition,
    WorldPosition returnPortalPosition,
    WorldPosition futureUpgradeZone,
    WorldPosition futureStationZone
) {
}
