package com.tavall.hytale.resourcegame.world;

import com.hypixel.hytale.math.vector.Vector3d;
import com.tavall.hytale.resourcegame.domain.CastleLocationData;

import java.util.List;

/**
 * Computes anchor positions for the readable main-world castle scene.
 */
public final class CastleSiteLayoutService {
    public CastleSiteLayout createLayout(CastleLocationData castleLocation) {
        double baseX = castleLocation.x();
        double baseY = Math.floor(castleLocation.y());
        double baseZ = castleLocation.z();

        Vector3d origin = new Vector3d(baseX, baseY, baseZ);
        Vector3d stockpileAnchor = new Vector3d(baseX, baseY + 1.0, baseZ + 4.0);
        Vector3d citizenAnchor = new Vector3d(baseX + 5.0, baseY + 1.0, baseZ + 1.5);
        Vector3d troopAnchor = new Vector3d(baseX - 5.0, baseY + 1.0, baseZ + 1.5);
        Vector3d foodNodeAnchor = new Vector3d(baseX + 7.0, baseY + 1.0, baseZ + 6.0);
        Vector3d woodNodeAnchor = new Vector3d(baseX - 7.0, baseY + 1.0, baseZ + 6.0);
        Vector3d ironNodeAnchor = new Vector3d(baseX, baseY + 1.0, baseZ + 9.0);

        List<Vector3d> stockpilePositions = List.of(
                new Vector3d(baseX - 1.2, baseY + 1.0, baseZ + 3.2),
                new Vector3d(baseX + 1.2, baseY + 1.0, baseZ + 3.2),
                new Vector3d(baseX - 1.2, baseY + 1.0, baseZ + 4.8),
                new Vector3d(baseX + 1.2, baseY + 1.0, baseZ + 4.8),
                new Vector3d(baseX - 1.6, baseY + 1.0, baseZ + 4.0),
                new Vector3d(baseX + 1.6, baseY + 1.0, baseZ + 4.0)
        );

        List<Vector3d> citizenCrowdPositions = List.of(
                new Vector3d(baseX + 4.0, baseY + 1.0, baseZ + 3.0),
                new Vector3d(baseX + 5.5, baseY + 1.0, baseZ + 3.4),
                new Vector3d(baseX + 6.5, baseY + 1.0, baseZ + 2.5),
                new Vector3d(baseX + 4.2, baseY + 1.0, baseZ + 4.5),
                new Vector3d(baseX + 5.8, baseY + 1.0, baseZ + 4.9),
                new Vector3d(baseX + 6.8, baseY + 1.0, baseZ + 3.8)
        );
        List<Vector3d> troopCrowdPositions = List.of(
                new Vector3d(baseX - 4.0, baseY + 1.0, baseZ + 3.0),
                new Vector3d(baseX - 5.5, baseY + 1.0, baseZ + 3.4),
                new Vector3d(baseX - 6.5, baseY + 1.0, baseZ + 2.5),
                new Vector3d(baseX - 4.2, baseY + 1.0, baseZ + 4.5),
                new Vector3d(baseX - 5.8, baseY + 1.0, baseZ + 4.9),
                new Vector3d(baseX - 6.8, baseY + 1.0, baseZ + 3.8)
        );
        List<Vector3d> foodNodePositions = List.of(
                new Vector3d(baseX + 8.2, baseY + 1.0, baseZ + 5.4),
                new Vector3d(baseX + 6.2, baseY + 1.0, baseZ + 5.2),
                new Vector3d(baseX + 7.8, baseY + 1.0, baseZ + 7.1),
                new Vector3d(baseX + 6.4, baseY + 1.0, baseZ + 7.0)
        );
        List<Vector3d> woodNodePositions = List.of(
                new Vector3d(baseX - 8.2, baseY + 1.0, baseZ + 5.4),
                new Vector3d(baseX - 6.2, baseY + 1.0, baseZ + 5.2),
                new Vector3d(baseX - 7.8, baseY + 1.0, baseZ + 7.1),
                new Vector3d(baseX - 6.4, baseY + 1.0, baseZ + 7.0)
        );
        List<Vector3d> ironNodePositions = List.of(
                new Vector3d(baseX - 1.2, baseY + 1.0, baseZ + 10.0),
                new Vector3d(baseX + 1.2, baseY + 1.0, baseZ + 10.0),
                new Vector3d(baseX - 1.5, baseY + 1.0, baseZ + 8.0),
                new Vector3d(baseX + 1.5, baseY + 1.0, baseZ + 8.0)
        );
        List<Vector3d> foodConvoyPositions = List.of(
                new Vector3d(baseX + 5.8, baseY + 1.0, baseZ + 5.2),
                new Vector3d(baseX + 4.1, baseY + 1.0, baseZ + 4.7),
                new Vector3d(baseX + 2.4, baseY + 1.0, baseZ + 4.2),
                new Vector3d(baseX + 0.8, baseY + 1.0, baseZ + 3.8)
        );
        List<Vector3d> woodConvoyPositions = List.of(
                new Vector3d(baseX - 5.8, baseY + 1.0, baseZ + 5.2),
                new Vector3d(baseX - 4.1, baseY + 1.0, baseZ + 4.7),
                new Vector3d(baseX - 2.4, baseY + 1.0, baseZ + 4.2),
                new Vector3d(baseX - 0.8, baseY + 1.0, baseZ + 3.8)
        );
        List<Vector3d> ironConvoyPositions = List.of(
                new Vector3d(baseX - 0.8, baseY + 1.0, baseZ + 7.9),
                new Vector3d(baseX + 0.8, baseY + 1.0, baseZ + 7.2),
                new Vector3d(baseX - 0.7, baseY + 1.0, baseZ + 6.1),
                new Vector3d(baseX + 0.7, baseY + 1.0, baseZ + 5.0)
        );

        return new CastleSiteLayout(
                origin,
                stockpileAnchor,
                citizenAnchor,
                troopAnchor,
                foodNodeAnchor,
                woodNodeAnchor,
                ironNodeAnchor,
                stockpilePositions,
                citizenCrowdPositions,
                troopCrowdPositions,
                foodNodePositions,
                woodNodePositions,
                ironNodePositions,
                foodConvoyPositions,
                woodConvoyPositions,
                ironConvoyPositions
        );
    }
}
