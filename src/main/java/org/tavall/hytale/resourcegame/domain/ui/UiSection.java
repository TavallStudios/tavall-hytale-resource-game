package org.tavall.hytale.resourcegame.domain.ui;

import java.util.List;

public record UiSection(String title, List<String> lines, List<UiAction> actions, boolean placeholder) {
}
