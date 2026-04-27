package com.tavall.hytale.resourcegame.ui;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.tavall.hytale.resourcegame.dependency.interfaces.IUiActionService;
import com.tavall.hytale.resourcegame.domain.CastleEconomySnapshot;
import com.tavall.hytale.resourcegame.domain.CitizenJobType;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.domain.UiNavigationContext;
import com.tavall.hytale.resourcegame.services.CastleEconomyPlanner;

import java.util.List;
import java.util.Map;

/**
 * Citizens placeholder page.
 */
public final class CastleCitizensPage extends BaseUiPage {
    private static final String PAGE_DOCUMENT = "Pages/castle-citizens.html";

    public CastleCitizensPage(
            Player player,
            UiNavigationContext context,
            PlayerGameState state,
            IUiActionService actionService,
            CastleEconomyPlanner economyPlanner
    ) {
        super(player, context, state, actionService, PAGE_DOCUMENT, templateData(context, state, economyPlanner), bindings());
    }

    private static Map<String, ?> templateData(UiNavigationContext context, PlayerGameState state, CastleEconomyPlanner economyPlanner) {
        CastleEconomySnapshot snapshot = economyPlanner.snapshot(state);
        return Map.ofEntries(
                Map.entry("CitizenCount", String.valueOf(state.populationSummary().citizenCount())),
                Map.entry("IdleCount", String.valueOf(snapshot.jobCount(CitizenJobType.IDLE))),
                Map.entry("GathererCount", String.valueOf(snapshot.jobCount(CitizenJobType.GATHERER))),
                Map.entry("HunterCount", String.valueOf(snapshot.jobCount(CitizenJobType.HUNTER))),
                Map.entry("CookCount", String.valueOf(snapshot.jobCount(CitizenJobType.COOK))),
                Map.entry("MinerCount", String.valueOf(snapshot.jobCount(CitizenJobType.MINER))),
                Map.entry("BuilderCount", String.valueOf(builderSpecialistCount(snapshot))),
                Map.entry("BlacksmithCount", String.valueOf(snapshot.jobCount(CitizenJobType.BLACKSMITH))),
                Map.entry("ArchitectCount", String.valueOf(snapshot.jobCount(CitizenJobType.ARCHITECT))),
                Map.entry("GruntBuilderCount", String.valueOf(snapshot.jobCount(CitizenJobType.GRUNT_BUILDER))),
                Map.entry("TraineeCount", String.valueOf(snapshot.jobCount(CitizenJobType.TRAINEE))),
                Map.entry("FeedbackStatus", context.feedbackMessage().isBlank() ? "Right-click an interior worker anchor to inspect that worker type." : context.feedbackMessage())
        );
    }

    private static int builderSpecialistCount(CastleEconomySnapshot snapshot) {
        return snapshot.jobCount(CitizenJobType.BLACKSMITH)
                + snapshot.jobCount(CitizenJobType.ARCHITECT)
                + snapshot.jobCount(CitizenJobType.GRUNT_BUILDER)
                + snapshot.jobCount(CitizenJobType.BUILDER);
    }

    private static List<HyUiActionBinding> bindings() {
        return List.of(HyUiActionBinding.action("#BackButton", UiActions.OPEN_CASTLE_MAIN));
    }
}
