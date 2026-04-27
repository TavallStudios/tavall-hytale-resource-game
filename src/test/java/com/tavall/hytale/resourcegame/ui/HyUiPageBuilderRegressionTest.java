package com.tavall.hytale.resourcegame.ui;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public final class HyUiPageBuilderRegressionTest {
    @Test
    void hyUiPagesBindEveryExpectedActionSelector() {
        assertPageBuilds("Pages/castle-main.html", castleMainBindings());
        assertPageBuilds("Pages/castle-info.html", castleInfoBindings());
        assertPageBuilds("Pages/castle-citizens.html", List.of(HyUiActionBinding.action("#BackButton", UiActions.OPEN_CASTLE_MAIN)));
        assertPageBuilds("Pages/castle-troops.html", List.of(HyUiActionBinding.action("#BackButton", UiActions.OPEN_CASTLE_MAIN)));
        assertPageBuilds("Pages/castle-resources.html", List.of(HyUiActionBinding.action("#BackButton", UiActions.OPEN_CASTLE_MAIN)));
        assertPageBuilds("Pages/castle-upgrades.html", castleUpgradeBindings());
        assertPageBuilds("Pages/castle-buildings.html", castleBuildingBindings());
        assertPageBuilds("Pages/building-detail.html", buildingDetailBindings());
        assertPageBuilds("Pages/resource-node-detail.html", resourceNodeBindings());
        assertPageBuilds("Pages/interior-main.html", interiorBindings());
        assertPageBuilds("Pages/debug-navigator.html", debugBindings());
    }

    private static void assertPageBuilds(String resourcePath, List<HyUiActionBinding> bindings) {
        HyUiPageDefinition definition = ResourceGameHyUiPageBuilder.build(
                resourcePath,
                Map.of(),
                bindings,
                (eventData, uiContext) -> {
                }
        );
        assertFalse(definition.topLevelElements().isEmpty(), () -> "No top-level HYUIML elements for " + resourcePath);
        assertNotNull(definition.templateHtml(), () -> "HyUI template HTML is missing for " + resourcePath);
    }

    private static List<HyUiActionBinding> castleMainBindings() {
        return List.of(
                HyUiActionBinding.action("#EnterInteriorButton", UiActions.ENTER_INTERIOR),
                HyUiActionBinding.action("#CastleInfoButton", UiActions.OPEN_CASTLE_INFO),
                HyUiActionBinding.action("#CitizensButton", UiActions.OPEN_CITIZENS),
                HyUiActionBinding.action("#TroopsButton", UiActions.OPEN_TROOPS),
                HyUiActionBinding.action("#ResourcesButton", UiActions.OPEN_RESOURCES),
                HyUiActionBinding.action("#UpgradesButton", UiActions.OPEN_UPGRADES),
                HyUiActionBinding.action("#BuildingsButton", UiActions.OPEN_BUILDINGS),
                HyUiActionBinding.action("#AttackButton", UiActions.CASTLE_ATTACK_PLACEHOLDER),
                HyUiActionBinding.action("#FriendButton", UiActions.CASTLE_FRIEND_PLACEHOLDER),
                HyUiActionBinding.action("#GuildButton", UiActions.CASTLE_GUILD_PLACEHOLDER),
                HyUiActionBinding.action("#CloseButton", UiActions.CLOSE)
        );
    }

    private static List<HyUiActionBinding> castleInfoBindings() {
        return List.of(
                HyUiActionBinding.action("#AttackButton", UiActions.CASTLE_ATTACK_PLACEHOLDER),
                HyUiActionBinding.action("#FriendButton", UiActions.CASTLE_FRIEND_PLACEHOLDER),
                HyUiActionBinding.action("#GuildButton", UiActions.CASTLE_GUILD_PLACEHOLDER),
                HyUiActionBinding.action("#BackButton", UiActions.OPEN_CASTLE_MAIN)
        );
    }

    private static List<HyUiActionBinding> castleUpgradeBindings() {
        return List.of(
                HyUiActionBinding.action("#PromoteButton", UiActions.PROMOTE),
                HyUiActionBinding.action("#DemoteButton", UiActions.DEMOTE),
                HyUiActionBinding.action("#BackButton", UiActions.OPEN_CASTLE_MAIN)
        );
    }

    private static List<HyUiActionBinding> castleBuildingBindings() {
        return List.of(
                HyUiActionBinding.command("#StageFarmsteadButton", "/kd buildings stage farmstead"),
                HyUiActionBinding.command("#StageLumberMillButton", "/kd buildings stage lumber_mill"),
                HyUiActionBinding.command("#StageIronWorksButton", "/kd buildings stage iron_works"),
                HyUiActionBinding.command("#StageBarracksButton", "/kd buildings stage barracks"),
                HyUiActionBinding.command("#StageWorkshopButton", "/kd buildings stage workshop"),
                HyUiActionBinding.action("#BackButton", UiActions.OPEN_CASTLE_MAIN)
        );
    }

    private static List<HyUiActionBinding> buildingDetailBindings() {
        return List.of(
                HyUiActionBinding.action("#StartUpgradeButton", UiActions.BUILDING_START_UPGRADE),
                HyUiActionBinding.action("#CancelUpgradeButton", UiActions.BUILDING_CANCEL_UPGRADE),
                HyUiActionBinding.action("#OverviewButton", UiActions.OPEN_BUILDINGS),
                HyUiActionBinding.action("#BackButton", UiActions.OPEN_CASTLE_MAIN)
        );
    }

    private static List<HyUiActionBinding> resourceNodeBindings() {
        return List.of(
                HyUiActionBinding.action("#AssignOneButton", UiActions.NODE_ASSIGN_ONE),
                HyUiActionBinding.action("#AssignThreeButton", UiActions.NODE_ASSIGN_THREE),
                HyUiActionBinding.action("#AssignFiveButton", UiActions.NODE_ASSIGN_FIVE),
                HyUiActionBinding.action("#AssignAllButton", UiActions.NODE_ASSIGN_ALL),
                HyUiActionBinding.action("#RecallOneButton", UiActions.NODE_RECALL_ONE),
                HyUiActionBinding.action("#RecallAllButton", UiActions.NODE_RECALL_ALL),
                HyUiActionBinding.action("#PillageButton", UiActions.NODE_PILLAGE),
                HyUiActionBinding.action("#BackButton", UiActions.OPEN_RESOURCES)
        );
    }

    private static List<HyUiActionBinding> interiorBindings() {
        return List.of(
                HyUiActionBinding.action("#ExitInteriorButton", UiActions.EXIT_INTERIOR),
                HyUiActionBinding.action("#BuildingsButton", UiActions.OPEN_BUILDINGS),
                HyUiActionBinding.action("#BackButton", UiActions.OPEN_CASTLE_MAIN)
        );
    }

    private static List<HyUiActionBinding> debugBindings() {
        return List.of(
                HyUiActionBinding.action("#CastleMainButton", UiActions.OPEN_CASTLE_MAIN),
                HyUiActionBinding.action("#CastleInfoButton", UiActions.OPEN_CASTLE_INFO),
                HyUiActionBinding.action("#CitizensButton", UiActions.OPEN_CITIZENS),
                HyUiActionBinding.action("#TroopsButton", UiActions.OPEN_TROOPS),
                HyUiActionBinding.action("#ResourcesButton", UiActions.OPEN_RESOURCES),
                HyUiActionBinding.action("#UpgradesButton", UiActions.OPEN_UPGRADES),
                HyUiActionBinding.action("#InteriorButton", UiActions.ENTER_INTERIOR),
                HyUiActionBinding.command("#PlaceCastleButton", "/kd place castle"),
                HyUiActionBinding.command("#PlaceFoodNodeButton", "/kd place node food"),
                HyUiActionBinding.command("#PlaceWoodNodeButton", "/kd place node wood"),
                HyUiActionBinding.command("#PlaceIronNodeButton", "/kd place node iron"),
                HyUiActionBinding.command("#ConfirmPlacementButton", "/kd place confirm"),
                HyUiActionBinding.command("#CancelPlacementButton", "/kd place cancel"),
                HyUiActionBinding.command("#MoveNegXButton", "/kd place move -1 0"),
                HyUiActionBinding.command("#MovePosXButton", "/kd place move 1 0"),
                HyUiActionBinding.command("#MoveNegZButton", "/kd place move 0 -1"),
                HyUiActionBinding.command("#MovePosZButton", "/kd place move 0 1"),
                HyUiActionBinding.command("#InteriorRebuildButton", "/kd interior rebuild"),
                HyUiActionBinding.command("#InteriorMoveButton", "/kd interior move"),
                HyUiActionBinding.command("#InteriorExitButton", "/kd interior exit"),
                HyUiActionBinding.command("#SceneRefreshButton", "/kd scene refresh"),
                HyUiActionBinding.command("#NodesClearButton", "/kd nodes clear"),
                HyUiActionBinding.command("#NodesListButton", "/kd nodes list"),
                HyUiActionBinding.command("#BuildingsListButton", "/kd buildings list"),
                HyUiActionBinding.command("#StageFarmsteadButton", "/kd buildings stage farmstead"),
                HyUiActionBinding.command("#StageLumberMillButton", "/kd buildings stage lumber_mill"),
                HyUiActionBinding.command("#StageIronWorksButton", "/kd buildings stage iron_works"),
                HyUiActionBinding.command("#StageBarracksButton", "/kd buildings stage barracks"),
                HyUiActionBinding.command("#StageWorkshopButton", "/kd buildings stage workshop"),
                HyUiActionBinding.command("#FocusButton", "/kd focus"),
                HyUiActionBinding.command("#InteractButton", "/kd interact"),
                HyUiActionBinding.command("#HologramTestButton", "/kd hologram stack Test hologram|Second line"),
                HyUiActionBinding.command("#TutorialResetButton", "/kd tutorial reset"),
                HyUiActionBinding.action("#CloseButton", UiActions.CLOSE)
        );
    }
}
