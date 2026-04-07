package org.tavall.hytale.resourcegame.domain.castle;

import java.util.UUID;
import org.tavall.hytale.resourcegame.runtime.HytaleAssetId;

/**
 * Persistent castle record for each player owner.
 */
public class CastleRecord {

  private final UUID castleId;
  private final long ownerProfileId;
  private CastleLocation location;
  private HytaleAssetId visualAsset;

  public CastleRecord(UUID castleId, long ownerProfileId, CastleLocation location, HytaleAssetId visualAsset) {
    this.castleId = castleId;
    this.ownerProfileId = ownerProfileId;
    this.location = location;
    this.visualAsset = visualAsset;
  }

  public UUID castleId() {
    return castleId;
  }

  public long ownerProfileId() {
    return ownerProfileId;
  }

  public CastleLocation location() {
    return location;
  }

  public HytaleAssetId visualAsset() {
    return visualAsset;
  }

  public void relocate(CastleLocation nextLocation) {
    this.location = nextLocation;
  }

  public void replaceVisual(HytaleAssetId nextVisualAsset) {
    this.visualAsset = nextVisualAsset;
  }
}
