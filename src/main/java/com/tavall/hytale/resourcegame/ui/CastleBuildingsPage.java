package com.tavall.hytale.resourcegame.ui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleBuildingService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IUiActionService;
import com.tavall.hytale.resourcegame.domain.BuildingType;
import com.tavall.hytale.resourcegame.domain.CastleBuildingData;
import com.tavall.hytale.resourcegame.domain.CastleBuildingSummary;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.domain.UiNavigationContext;

import java.time.Instant;
import java.util.Optional;

/**
 * Overview page for castle and interior building progression.
 */
public final class CastleBuildingsPage extends BaseUiPage {
    private static final String PAGE_DOCUMENT = "Pages/castle-buildings.ui";
    private final ICastleBuildingService buildingService;

    public CastleBuildingsPage(
            Player player,
            UiNavigationContext context,
            PlayerGameState state,
            IUiActionService actionService,
            ICastleBuildingService buildingService
    ) {
        super(player, context, state, actionService);
        this.buildingService = buildingService;
    }

    @Override
    public void build(Ref<EntityStore> entityRef, UICommandBuilder uiCommandBuilder, UIEventBuilder uiEventBuilder, Store<EntityStore> entityStore) {
        uiCommandBuilder.append(PAGE_DOCUMENT);
        uiCommandBuilder.set("#FarmsteadStatus.Text", status(BuildingType.FARMSTEAD));
        uiCommandBuilder.set("#LumberMillStatus.Text", status(BuildingType.LUMBER_MILL));
        uiCommandBuilder.set("#IronWorksStatus.Text", status(BuildingType.IRON_WORKS));
        uiCommandBuilder.set("#BarracksStatus.Text", status(BuildingType.BARRACKS));
        uiCommandBuilder.set("#WorkshopStatus.Text", status(BuildingType.WORKSHOP));
        uiCommandBuilder.set(
                "#FooterStatus.Text",
                context().feedbackMessage().isBlank()
                        ? "Place buildings with /kd buildings place <type>, then interact with them in-world to open detail and start upgrades."
                        : context().feedbackMessage()
        );
        bindCommand(uiEventBuilder, "#StageFarmsteadButton", "/kd buildings stage farmstead");
        bindCommand(uiEventBuilder, "#StageLumberMillButton", "/kd buildings stage lumber_mill");
        bindCommand(uiEventBuilder, "#StageIronWorksButton", "/kd buildings stage iron_works");
        bindCommand(uiEventBuilder, "#StageBarracksButton", "/kd buildings stage barracks");
        bindCommand(uiEventBuilder, "#StageWorkshopButton", "/kd buildings stage workshop");
        bind(uiEventBuilder, "#BackButton", UiActions.OPEN_CASTLE_MAIN);
    }

    private String status(BuildingType buildingType) {
        Optional<CastleBuildingData> building = buildingService.resolveBuilding(state(), buildingType.shortKey());
        if (building.isEmpty()) {
            return "Missing | Place in " + buildingType.areaType().displayName() + " | " + buildingType.description();
        }
        CastleBuildingSummary summary = buildingService.summary(player().getUuid(), state(), building.get(), Instant.now());
        if (summary.isUnderConstruction()) {
            return "L" + summary.completedLevel()
                    + " -> L" + summary.displayLevel()
                    + " | " + summary.constructionStage().name().toLowerCase()
                    + " " + (int) Math.round(summary.progressRatio() * 100.0D) + "%"
                    + " | " + summary.remainingSeconds() + "s";
        }
        return "L" + summary.completedLevel()
                + " | " + effectSummary(summary)
                + (summary.nextUpgradeProfile() == null ? " | Maxed" : " | Ready for next upgrade");
    }

    private String effectSummary(CastleBuildingSummary summary) {
        if (summary.foodPerTickBonus() > 0 || summary.woodPerTickBonus() > 0 || summary.ironPerTickBonus() > 0) {
            return "+" + summary.foodPerTickBonus() + "F +" + summary.woodPerTickBonus() + "W +" + summary.ironPerTickBonus() + "I / tick";
        }
        if (summary.constructionSpeedBonus() > 0.0D) {
            return "Build speed +" + (int) Math.round(summary.constructionSpeedBonus() * 100.0D) + "%";
        }
        return "Promotion discount -" + summary.promotionDiscount().foodCost()
                + "/" + summary.promotionDiscount().woodCost()
                + "/" + summary.promotionDiscount().ironCost();
    }

    private void bind(UIEventBuilder uiEventBuilder, String selector, String action) {
        uiEventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                selector,
                EventData.of(UiActionEventData.KEY_ACTION, action),
                false
        );
    }

    private void bindCommand(UIEventBuilder uiEventBuilder, String selector, String commandLine) {
        uiEventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                selector,
                new EventData()
                        .put(UiActionEventData.KEY_ACTION, UiActions.RUN_COMMAND)
                        .put(UiActionEventData.KEY_PAYLOAD, commandLine),
                false
        );
    }
}
