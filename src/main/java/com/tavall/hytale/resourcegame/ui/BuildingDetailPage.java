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
import com.tavall.hytale.resourcegame.domain.CastleBuildingData;
import com.tavall.hytale.resourcegame.domain.CastleBuildingSummary;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.domain.UiNavigationContext;

import java.time.Instant;
import java.util.Optional;

/**
 * Detail page for one selected building.
 */
public final class BuildingDetailPage extends BaseUiPage {
    private static final String PAGE_DOCUMENT = "Pages/building-detail.ui";
    private final ICastleBuildingService buildingService;

    public BuildingDetailPage(
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
        Optional<CastleBuildingData> buildingOptional = buildingService.findBuilding(state(), context().selectedBuildingId());
        if (buildingOptional.isEmpty()) {
            uiCommandBuilder.set("#BuildingTitle.Text", "Building Missing");
            uiCommandBuilder.set("#AreaText.Text", "No tracked building is selected.");
            uiCommandBuilder.set("#LocationText.Text", "Use /kd buildings select <type|id> or interact with a placed building.");
            uiCommandBuilder.set("#LevelText.Text", "-");
            uiCommandBuilder.set("#StatusText.Text", "Missing");
            uiCommandBuilder.set("#EffectText.Text", "-");
            uiCommandBuilder.set("#NextUpgradeText.Text", "Select a building from the world first.");
            uiCommandBuilder.set("#FeedbackStatus.Text", context().feedbackMessage().isBlank() ? "Awaiting building selection." : context().feedbackMessage());
        } else {
            CastleBuildingSummary summary = buildingService.summary(player().getUuid(), state(), buildingOptional.get(), Instant.now());
            uiCommandBuilder.set("#BuildingTitle.Text", summary.buildingData().buildingType().displayName());
            uiCommandBuilder.set("#AreaText.Text", summary.buildingData().areaType().displayName());
            uiCommandBuilder.set(
                    "#LocationText.Text",
                    summary.worldName() + " | " + (int) summary.worldX() + ", " + (int) summary.worldY() + ", " + (int) summary.worldZ()
            );
            uiCommandBuilder.set(
                    "#LevelText.Text",
                    summary.isUnderConstruction()
                            ? "L" + summary.completedLevel() + " -> L" + summary.displayLevel()
                            : "L" + summary.completedLevel()
            );
            uiCommandBuilder.set(
                    "#StatusText.Text",
                    summary.isUnderConstruction()
                            ? summary.constructionStage().name().toLowerCase() + " | " + (int) Math.round(summary.progressRatio() * 100.0D) + "% | " + summary.remainingSeconds() + "s"
                            : summary.statusText()
            );
            uiCommandBuilder.set("#EffectText.Text", effectText(summary));
            uiCommandBuilder.set("#NextUpgradeText.Text", nextUpgradeText(summary));
            uiCommandBuilder.set("#FeedbackStatus.Text", context().feedbackMessage().isBlank() ? "Use the button below to start the next upgrade when resources allow." : context().feedbackMessage());
        }
        bind(uiEventBuilder, "#StartUpgradeButton", UiActions.BUILDING_START_UPGRADE);
        bind(uiEventBuilder, "#OverviewButton", UiActions.OPEN_BUILDINGS);
        bind(uiEventBuilder, "#BackButton", UiActions.OPEN_CASTLE_MAIN);
    }

    private String effectText(CastleBuildingSummary summary) {
        if (summary.foodPerTickBonus() > 0 || summary.woodPerTickBonus() > 0 || summary.ironPerTickBonus() > 0) {
            return "Passive yield: +" + summary.foodPerTickBonus() + " Food, +" + summary.woodPerTickBonus() + " Wood, +" + summary.ironPerTickBonus() + " Iron per tick.";
        }
        if (summary.constructionSpeedBonus() > 0.0D) {
            return "Construction effect: +" + (int) Math.round(summary.constructionSpeedBonus() * 100.0D) + "% faster future building work.";
        }
        return "Training effect: promotion discount -" + summary.promotionDiscount().foodCost()
                + " Food, -" + summary.promotionDiscount().woodCost()
                + " Wood, -" + summary.promotionDiscount().ironCost()
                + " Iron.";
    }

    private String nextUpgradeText(CastleBuildingSummary summary) {
        if (summary.isUnderConstruction()) {
            return "Current work order finishes in " + summary.remainingSeconds() + "s.";
        }
        if (summary.nextUpgradeProfile() == null) {
            return "Max level reached.";
        }
        return "Next upgrade cost: "
                + summary.nextUpgradeProfile().foodCost() + " Food, "
                + summary.nextUpgradeProfile().woodCost() + " Wood, "
                + summary.nextUpgradeProfile().ironCost() + " Iron | Build time "
                + summary.nextUpgradeProfile().buildSeconds() + "s before workshop bonuses.";
    }

    private void bind(UIEventBuilder uiEventBuilder, String selector, String action) {
        uiEventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                selector,
                EventData.of(UiActionEventData.KEY_ACTION, action),
                false
        );
    }
}
