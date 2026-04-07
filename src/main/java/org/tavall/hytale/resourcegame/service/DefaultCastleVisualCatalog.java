package org.tavall.hytale.resourcegame.service;

import org.tavall.hytale.resourcegame.runtime.HytaleAssetId;

public class DefaultCastleVisualCatalog implements CastleVisualCatalog {

  @Override
  public HytaleAssetId initialCastleAsset() {
    return HytaleAssetId.CASTLE_STONE_TIER_ONE;
  }
}
