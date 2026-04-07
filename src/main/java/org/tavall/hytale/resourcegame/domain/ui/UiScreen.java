package org.tavall.hytale.resourcegame.domain.ui;

import java.util.List;
import org.tavall.hytale.resourcegame.runtime.HytaleAssetId;

public record UiScreen(String screenId, String title, HytaleAssetId frameAsset, List<UiSection> sections) {
}
