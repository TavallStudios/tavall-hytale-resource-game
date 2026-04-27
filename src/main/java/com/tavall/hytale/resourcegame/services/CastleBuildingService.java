package com.tavall.hytale.resourcegame.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hypixel.hytale.math.vector.Vector3d;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.dependency.interfaces.ICastleBuildingService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IInteriorInstanceService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerGameStateService;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlayerSessionStore;
import com.tavall.hytale.resourcegame.domain.AgingState;
import com.tavall.hytale.resourcegame.domain.AccountProgression;
import com.tavall.hytale.resourcegame.domain.BuildingAreaType;
import com.tavall.hytale.resourcegame.domain.BuildingConstructionStage;
import com.tavall.hytale.resourcegame.domain.BuildingLevelProfile;
import com.tavall.hytale.resourcegame.domain.BuildingMutationResult;
import com.tavall.hytale.resourcegame.domain.BuildingType;
import com.tavall.hytale.resourcegame.domain.CastleBuildingData;
import com.tavall.hytale.resourcegame.domain.CastleBuildingSummary;
import com.tavall.hytale.resourcegame.domain.GameStateMetadata;
import com.tavall.hytale.resourcegame.domain.OnboardingProgress;
import com.tavall.hytale.resourcegame.domain.PlayerGameState;
import com.tavall.hytale.resourcegame.domain.ResourceInventory;
import com.tavall.hytale.resourcegame.domain.ResourceNodeData;
import com.tavall.hytale.resourcegame.interior.InteriorLayout;
import com.tavall.hytale.resourcegame.interior.InteriorLayoutService;
import com.tavall.hytale.resourcegame.population.PromotionCost;
import com.tavall.hytale.resourcegame.tasks.AsyncTask;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Owns upgradeable buildings tied to the castle surface or the interior world.
 */
public final class CastleBuildingService implements ICastleBuildingService, IDependencyInjectableConcrete {
    private static final Logger LOGGER = Logger.getLogger(CastleBuildingService.class.getName());
    private static final double SURFACE_MIN_RADIUS = 5.0D;
    private static final double SURFACE_MAX_RADIUS = 18.0D;
    private static final double INTERIOR_MAX_RADIUS = 22.0D;
    private static final double BUILDING_MIN_SPACING = 4.25D;
    private static final double INTERIOR_ANCHOR_CLEARANCE = 2.5D;

    private final IPlayerSessionStore sessionStore;
    private final IPlayerGameStateService gameStateService;
    private final IInteriorInstanceService interiorInstanceService;
    private final InteriorLayoutService interiorLayoutService;
    private final ObjectMapper objectMapper;

    public CastleBuildingService(
            IPlayerSessionStore sessionStore,
            IPlayerGameStateService gameStateService,
            IInteriorInstanceService interiorInstanceService,
            InteriorLayoutService interiorLayoutService,
            ObjectMapper objectMapper
    ) {
        this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore");
        this.gameStateService = Objects.requireNonNull(gameStateService, "gameStateService");
        this.interiorInstanceService = Objects.requireNonNull(interiorInstanceService, "interiorInstanceService");
        this.interiorLayoutService = Objects.requireNonNull(interiorLayoutService, "interiorLayoutService");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    @Override
    public List<CastleBuildingData> listBuildings(PlayerGameState state) {
        return metadataOf(state, resolveNow(state)).castleBuildings().stream()
                .sorted(Comparator.comparing((CastleBuildingData value) -> value.areaType().ordinal())
                        .thenComparing(CastleBuildingData::placedAt)
                        .thenComparing(CastleBuildingData::buildingId))
                .toList();
    }

    @Override
    public Optional<CastleBuildingData> resolveBuilding(PlayerGameState state, String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        List<CastleBuildingData> buildings = listBuildings(state);
        try {
            int index = Integer.parseInt(token);
            if (index >= 1 && index <= buildings.size()) {
                return Optional.of(buildings.get(index - 1));
            }
        } catch (NumberFormatException ignored) {
        }
        BuildingType buildingType = BuildingType.parse(token);
        if (buildingType != null) {
            Optional<CastleBuildingData> byType = buildings.stream()
                    .filter(building -> building.buildingType() == buildingType)
                    .findFirst();
            if (byType.isPresent()) {
                return byType;
            }
        }
        String normalized = token.toLowerCase(Locale.ROOT).replace("-", "");
        return buildings.stream()
                .filter(building -> building.buildingId().toString().replace("-", "").startsWith(normalized))
                .findFirst();
    }

    @Override
    public Optional<CastleBuildingData> findBuilding(PlayerGameState state, UUID buildingId) {
        if (buildingId == null) {
            return Optional.empty();
        }
        return listBuildings(state).stream().filter(building -> building.buildingId().equals(buildingId)).findFirst();
    }

    @Override
    public CastleBuildingSummary summary(UUID playerId, PlayerGameState state, CastleBuildingData buildingData, Instant now) {
        Vector3d origin = areaOrigin(playerId, state, buildingData.areaType());
        String worldName = areaWorldName(playerId, state, buildingData.areaType());
        Vector3d worldPosition = new Vector3d(
                origin.getX() + buildingData.relativeX(),
                origin.getY() + buildingData.relativeY(),
                origin.getZ() + buildingData.relativeZ()
        );
        double progressRatio = progressRatio(buildingData, now);
        long remainingSeconds = remainingSeconds(buildingData, now);
        BuildingConstructionStage stage = BuildingConstructionStage.fromProgress(buildingData.isUnderConstruction(), progressRatio);
        BuildingLevelProfile completedProfile = buildingData.currentLevel() <= 0
                ? null
                : buildingData.buildingType().levelProfile(buildingData.currentLevel());
        BuildingLevelProfile targetProfile = buildingData.targetLevel() <= 0
                ? null
                : buildingData.buildingType().levelProfile(buildingData.targetLevel());
        BuildingLevelProfile nextProfile = buildingData.currentLevel() >= buildingData.buildingType().maxLevel()
                ? null
                : buildingData.buildingType().levelProfile(buildingData.currentLevel() + 1);
        return new CastleBuildingSummary(
                buildingData,
                worldName,
                worldPosition.getX(),
                worldPosition.getY(),
                worldPosition.getZ(),
                progressRatio,
                remainingSeconds,
                stage,
                completedProfile,
                targetProfile,
                nextProfile
        );
    }

    @Override
    public String summaryLine(UUID playerId, PlayerGameState state, CastleBuildingData buildingData, int index, Instant now) {
        CastleBuildingSummary summary = summary(playerId, state, buildingData, now);
        String levelText = summary.isUnderConstruction()
                ? "L" + summary.completedLevel() + " -> L" + summary.displayLevel()
                : "L" + summary.completedLevel();
        String progressText = summary.isUnderConstruction()
                ? " | " + summary.constructionStage().name().toLowerCase(Locale.ROOT)
                + " " + (int) Math.round(summary.progressRatio() * 100.0D) + "%"
                + " | " + summary.remainingSeconds() + "s left"
                : " | operational";
        return "#" + index + " " + buildingData.buildingType().displayName()
                + " " + shortId(buildingData.buildingId())
                + " | " + buildingData.areaType().displayName()
                + " | " + levelText
                + progressText
                + " | +" + summary.foodPerTickBonus() + "F +" + summary.woodPerTickBonus() + "W +" + summary.ironPerTickBonus() + "I"
                + " | speed " + percent(summary.constructionSpeedBonus())
                + " | promo -" + promotionDiscountLabel(summary.promotionDiscount());
    }

    @Override
    public BuildingMutationResult placeBuilding(UUID playerId, BuildingType buildingType, String worldName, Vector3d worldPosition, Instant now) {
        PlayerSession session = sessionStore.get(playerId);
        if (session == null) {
            return new BuildingMutationResult(null, false, "No active session.");
        }
        if (buildingType == null || worldName == null || worldPosition == null) {
            return BuildingMutationResult.unchanged(session.gameState(), "Building placement data is incomplete.");
        }
        PlayerGameState state = session.gameState();
        if (listBuildings(state).stream().anyMatch(building -> building.buildingType() == buildingType)) {
            return BuildingMutationResult.unchanged(state, buildingType.displayName() + " already exists.");
        }
        AccountProgression accountProgression = metadataOf(state, now).accountProgression();
        boolean ignoresLevelRestrictions = metadataOf(state, now).debugModeState().levelRestrictionsIgnored();
        if (!ignoresLevelRestrictions && !accountProgression.isUnlocked(buildingType)) {
            return BuildingMutationResult.unchanged(
                    state,
                    buildingType.displayName() + " unlocks at account level " + accountProgression.requiredLevel(buildingType) + '.'
            );
        }
        String blockedReason = validatePlacement(playerId, state, buildingType, worldName, worldPosition);
        if (blockedReason != null) {
            return BuildingMutationResult.unchanged(state, blockedReason);
        }
        BuildingLevelProfile levelOneProfile = buildingType.levelProfile(1);
        if (!hasResources(state.resources(), levelOneProfile)) {
            return BuildingMutationResult.unchanged(state, missingCostMessage(buildingType, levelOneProfile));
        }
        Vector3d origin = areaOrigin(playerId, state, buildingType.areaType());
        CastleBuildingData newBuilding = new CastleBuildingData(
                UUID.randomUUID(),
                buildingType,
                worldPosition.getX() - origin.getX(),
                worldPosition.getY() - origin.getY(),
                worldPosition.getZ() - origin.getZ(),
                0,
                1,
                now,
                now,
                now.plusSeconds(adjustedBuildSeconds(levelOneProfile.buildSeconds(), constructionSpeedMultiplier(state)))
        );
        List<CastleBuildingData> updatedBuildings = new ArrayList<>(listBuildings(state));
        updatedBuildings.add(newBuilding);
        PlayerGameState updatedState = rewriteBuildings(
                deductCost(state, levelOneProfile, now),
                updatedBuildings,
                now
        );
        persistSessionState(session, updatedState, now);
        return BuildingMutationResult.changed(updatedState, buildingType.displayName() + " construction started.");
    }

    @Override
    public BuildingMutationResult spawnBuilding(UUID playerId, BuildingType buildingType, int level, String worldName, Vector3d worldPosition, Instant now) {
        PlayerSession session = sessionStore.get(playerId);
        if (session == null) {
            return new BuildingMutationResult(null, false, "No active session.");
        }
        if (buildingType == null || worldName == null || worldPosition == null) {
            return BuildingMutationResult.unchanged(session.gameState(), "Building spawn data is incomplete.");
        }
        int safeLevel = Math.max(1, Math.min(buildingType.maxLevel(), level));
        PlayerGameState state = session.gameState();
        String blockedReason = validatePlacement(playerId, state, buildingType, worldName, worldPosition);
        if (blockedReason != null) {
            return BuildingMutationResult.unchanged(state, blockedReason);
        }
        Vector3d origin = areaOrigin(playerId, state, buildingType.areaType());
        List<CastleBuildingData> updatedBuildings = new ArrayList<>(listBuildings(state));
        for (int index = 0; index < updatedBuildings.size(); index++) {
            CastleBuildingData candidate = updatedBuildings.get(index);
            if (candidate.buildingType() != buildingType) {
                continue;
            }
            CastleBuildingData updatedBuilding = new CastleBuildingData(
                    candidate.buildingId(),
                    buildingType,
                    worldPosition.getX() - origin.getX(),
                    worldPosition.getY() - origin.getY(),
                    worldPosition.getZ() - origin.getZ(),
                    safeLevel,
                    safeLevel,
                    candidate.placedAt(),
                    null,
                    null
            );
            updatedBuildings.set(index, updatedBuilding);
            PlayerGameState updatedState = rewriteBuildings(state, updatedBuildings, now);
            persistSessionState(session, updatedState, now);
            return BuildingMutationResult.changed(updatedState, buildingType.displayName() + " set to level " + safeLevel + '.');
        }

        CastleBuildingData newBuilding = new CastleBuildingData(
                UUID.randomUUID(),
                buildingType,
                worldPosition.getX() - origin.getX(),
                worldPosition.getY() - origin.getY(),
                worldPosition.getZ() - origin.getZ(),
                safeLevel,
                safeLevel,
                now,
                null,
                null
        );
        updatedBuildings.add(newBuilding);
        PlayerGameState updatedState = rewriteBuildings(state, updatedBuildings, now);
        persistSessionState(session, updatedState, now);
        return BuildingMutationResult.changed(updatedState, buildingType.displayName() + " spawned at level " + safeLevel + '.');
    }

    @Override
    public BuildingMutationResult startUpgrade(UUID playerId, UUID buildingId, Instant now) {
        PlayerSession session = sessionStore.get(playerId);
        if (session == null) {
            return new BuildingMutationResult(null, false, "No active session.");
        }
        PlayerGameState state = session.gameState();
        Optional<CastleBuildingData> buildingOptional = findBuilding(state, buildingId);
        if (buildingOptional.isEmpty()) {
            return BuildingMutationResult.unchanged(state, "Building not found.");
        }
        CastleBuildingData building = buildingOptional.get();
        if (building.isUnderConstruction()) {
            return BuildingMutationResult.unchanged(state, building.buildingType().displayName() + " is already under construction.");
        }
        if (building.currentLevel() >= building.buildingType().maxLevel()) {
            return BuildingMutationResult.unchanged(state, building.buildingType().displayName() + " is already maxed.");
        }
        BuildingLevelProfile targetProfile = building.buildingType().levelProfile(building.currentLevel() + 1);
        if (!hasResources(state.resources(), targetProfile)) {
            return BuildingMutationResult.unchanged(state, missingCostMessage(building.buildingType(), targetProfile));
        }
        List<CastleBuildingData> updatedBuildings = new ArrayList<>(listBuildings(state));
        for (int index = 0; index < updatedBuildings.size(); index++) {
            CastleBuildingData candidate = updatedBuildings.get(index);
            if (!candidate.buildingId().equals(buildingId)) {
                continue;
            }
            updatedBuildings.set(
                    index,
                    candidate.withConstruction(
                            candidate.currentLevel() + 1,
                            now,
                            now.plusSeconds(adjustedBuildSeconds(targetProfile.buildSeconds(), constructionSpeedMultiplier(state)))
                    )
            );
            PlayerGameState updatedState = rewriteBuildings(
                    deductCost(state, targetProfile, now),
                    updatedBuildings,
                    now
            );
            persistSessionState(session, updatedState, now);
            return BuildingMutationResult.changed(updatedState, building.buildingType().displayName() + " upgrade started.");
        }
        return BuildingMutationResult.unchanged(state, "Building not found.");
    }

    @Override
    public BuildingMutationResult cancelUpgrade(UUID playerId, UUID buildingId, Instant now) {
        PlayerSession session = sessionStore.get(playerId);
        if (session == null) {
            return new BuildingMutationResult(null, false, "No active session.");
        }
        PlayerGameState state = session.gameState();
        List<CastleBuildingData> updatedBuildings = new ArrayList<>(listBuildings(state));
        for (int index = 0; index < updatedBuildings.size(); index++) {
            CastleBuildingData building = updatedBuildings.get(index);
            if (!building.buildingId().equals(buildingId)) {
                continue;
            }
            if (!building.isUnderConstruction()) {
                return BuildingMutationResult.unchanged(state, building.buildingType().displayName() + " has no active upgrade.");
            }
            BuildingLevelProfile targetProfile = building.buildingType().levelProfile(building.targetLevel());
            ResourceInventory refunded = state.resources()
                    .withFood(state.resources().food() + targetProfile.foodCost())
                    .withWood(state.resources().wood() + targetProfile.woodCost())
                    .withIron(state.resources().iron() + targetProfile.ironCost());
            updatedBuildings.set(index, building.cancelConstruction());
            PlayerGameState refundedState = state.withResources(refunded, now);
            PlayerGameState updatedState = rewriteBuildings(refundedState, updatedBuildings, now);
            persistSessionState(session, updatedState, now);
            return BuildingMutationResult.changed(
                    updatedState,
                    building.buildingType().displayName() + " upgrade canceled. Refunded "
                            + targetProfile.foodCost() + "F "
                            + targetProfile.woodCost() + "W "
                            + targetProfile.ironCost() + "I."
            );
        }
        return BuildingMutationResult.unchanged(state, "Building not found.");
    }

    @Override
    public BuildingMutationResult forceComplete(UUID playerId, UUID buildingId, Instant now) {
        PlayerSession session = sessionStore.get(playerId);
        if (session == null) {
            return new BuildingMutationResult(null, false, "No active session.");
        }
        PlayerGameState state = session.gameState();
        List<CastleBuildingData> updatedBuildings = new ArrayList<>(listBuildings(state));
        for (int index = 0; index < updatedBuildings.size(); index++) {
            CastleBuildingData building = updatedBuildings.get(index);
            if (!building.buildingId().equals(buildingId)) {
                continue;
            }
            if (!building.isUnderConstruction()) {
                return BuildingMutationResult.unchanged(state, building.buildingType().displayName() + " is already operational.");
            }
            updatedBuildings.set(index, building.completeConstruction(now));
            PlayerGameState updatedState = rewriteBuildings(state, updatedBuildings, now);
            persistSessionState(session, updatedState, now);
            return BuildingMutationResult.changed(updatedState, building.buildingType().displayName() + " completed instantly.");
        }
        return BuildingMutationResult.unchanged(state, "Building not found.");
    }

    @Override
    public BuildingMutationResult clearBuildings(UUID playerId, Instant now) {
        PlayerSession session = sessionStore.get(playerId);
        if (session == null) {
            return new BuildingMutationResult(null, false, "No active session.");
        }
        PlayerGameState updatedState = rewriteBuildings(session.gameState(), List.of(), now);
        persistSessionState(session, updatedState, now);
        return BuildingMutationResult.changed(updatedState, "All kingdom buildings cleared.");
    }

    @Override
    public PlayerGameState applyTick(UUID playerId, PlayerGameState state, Instant now) {
        List<CastleBuildingData> buildings = listBuildings(state);
        if (buildings.isEmpty()) {
            return state;
        }
        boolean changed = false;
        List<CastleBuildingData> updatedBuildings = new ArrayList<>();
        for (CastleBuildingData building : buildings) {
            if (building.isUnderConstruction() && !building.constructionEndsAt().isAfter(now)) {
                updatedBuildings.add(building.completeConstruction(now));
                changed = true;
            } else {
                updatedBuildings.add(building);
            }
        }
        PlayerGameState currentState = changed ? rewriteBuildings(state, updatedBuildings, now) : state;
        ResourceInventory resources = currentState.resources();
        int food = resources.food();
        int wood = resources.wood();
        int iron = resources.iron();
        for (CastleBuildingData building : updatedBuildings) {
            if (building.isUnderConstruction() || building.currentLevel() <= 0) {
                continue;
            }
            BuildingLevelProfile profile = building.buildingType().levelProfile(building.currentLevel());
            food += profile.foodPerTickBonus();
            wood += profile.woodPerTickBonus();
            iron += profile.ironPerTickBonus();
        }
        if (food == resources.food() && wood == resources.wood() && iron == resources.iron()) {
            return currentState;
        }
        return currentState.withResources(new ResourceInventory(food, wood, iron), now);
    }

    @Override
    public PromotionCost adjustedPromotionCost(PlayerGameState state, PromotionCost baseCost) {
        int foodDiscount = 0;
        int woodDiscount = 0;
        int ironDiscount = 0;
        for (CastleBuildingData building : listBuildings(state)) {
            if (building.isUnderConstruction() || building.currentLevel() <= 0) {
                continue;
            }
            PromotionCost promotionDiscount = building.buildingType().levelProfile(building.currentLevel()).promotionDiscount();
            foodDiscount += promotionDiscount.foodCost();
            woodDiscount += promotionDiscount.woodCost();
            ironDiscount += promotionDiscount.ironCost();
        }
        return new PromotionCost(
                Math.max(0, baseCost.foodCost() - foodDiscount),
                Math.max(0, baseCost.woodCost() - woodDiscount),
                Math.max(0, baseCost.ironCost() - ironDiscount)
        );
    }

    @Override
    public double constructionSpeedMultiplier(PlayerGameState state) {
        double totalBonus = 0.0D;
        for (CastleBuildingData building : listBuildings(state)) {
            if (building.isUnderConstruction() || building.currentLevel() <= 0) {
                continue;
            }
            totalBonus += building.buildingType().levelProfile(building.currentLevel()).constructionSpeedBonus();
        }
        return Math.max(0.35D, 1.0D - totalBonus);
    }

    private String validatePlacement(UUID playerId, PlayerGameState state, BuildingType buildingType, String worldName, Vector3d worldPosition) {
        Vector3d origin = areaOrigin(playerId, state, buildingType.areaType());
        String expectedWorldName = areaWorldName(playerId, state, buildingType.areaType());
        if (expectedWorldName == null || !expectedWorldName.equals(worldName)) {
            return buildingType.areaType() == BuildingAreaType.CASTLE_SURFACE
                    ? "Surface buildings must be placed in the castle world."
                    : "Interior buildings must be placed inside the interior world.";
        }
        Vector3d relative = new Vector3d(
                worldPosition.getX() - origin.getX(),
                worldPosition.getY() - origin.getY(),
                worldPosition.getZ() - origin.getZ()
        );
        double horizontalRadius = Math.sqrt((relative.getX() * relative.getX()) + (relative.getZ() * relative.getZ()));
        if (buildingType.areaType() == BuildingAreaType.CASTLE_SURFACE) {
            if (horizontalRadius < SURFACE_MIN_RADIUS || horizontalRadius > SURFACE_MAX_RADIUS) {
                return "Place " + buildingType.displayName() + " between 5 and 18 blocks from the castle center.";
            }
            if (Math.abs(relative.getX()) < 4.5D && relative.getZ() < 8.0D) {
                return "Keep the central castle lane clear. Place that building further around the outer yard.";
            }
        } else {
            if (horizontalRadius > INTERIOR_MAX_RADIUS) {
                return "Place interior buildings inside the buildable shell.";
            }
            InteriorLayout layout = interiorLayoutService.createLayoutForPlayer(playerId);
            if (distance(worldPosition, layout.entryPoint()) < INTERIOR_ANCHOR_CLEARANCE
                    || distance(worldPosition, layout.exitPoint()) < INTERIOR_ANCHOR_CLEARANCE
                    || distance(worldPosition, layout.citizenAnchor()) < INTERIOR_ANCHOR_CLEARANCE
                    || distance(worldPosition, layout.troopAnchor()) < INTERIOR_ANCHOR_CLEARANCE) {
                return "That spot blocks an interior lane or anchor. Move it away from the entry, exit, citizen, or troop anchors.";
            }
        }
        for (CastleBuildingData building : listBuildings(state)) {
            if (building.areaType() != buildingType.areaType()) {
                continue;
            }
            CastleBuildingSummary summary = summary(playerId, state, building, resolveNow(state));
            if (distance(worldPosition, new Vector3d(summary.worldX(), summary.worldY(), summary.worldZ())) < BUILDING_MIN_SPACING) {
                return "That spot is too close to another building.";
            }
        }
        return null;
    }

    private ResourceInventory deductCost(ResourceInventory resources, BuildingLevelProfile profile) {
        return resources
                .withFood(resources.food() - profile.foodCost())
                .withWood(resources.wood() - profile.woodCost())
                .withIron(resources.iron() - profile.ironCost());
    }

    private PlayerGameState deductCost(PlayerGameState state, BuildingLevelProfile profile, Instant now) {
        return state.withResources(deductCost(state.resources(), profile), now);
    }

    private boolean hasResources(ResourceInventory resources, BuildingLevelProfile profile) {
        return resources.food() >= profile.foodCost()
                && resources.wood() >= profile.woodCost()
                && resources.iron() >= profile.ironCost();
    }

    private String missingCostMessage(BuildingType buildingType, BuildingLevelProfile profile) {
        return "Need " + profile.foodCost() + " Food, "
                + profile.woodCost() + " Wood, and "
                + profile.ironCost() + " Iron to start " + buildingType.displayName() + ".";
    }

    private long adjustedBuildSeconds(int baseSeconds, double multiplier) {
        return Math.max(6L, (long) Math.ceil(baseSeconds * multiplier));
    }

    private void persistSessionState(PlayerSession session, PlayerGameState updatedState, Instant now) {
        session.updateGameState(updatedState);
        gameStateService.cacheState(session.playerId(), updatedState);
        AsyncTask.runAsync(() -> gameStateService.persistState(updatedState, now));
        LOGGER.info(() -> "Castle buildings updated for " + session.playerId() + ": " + listBuildings(updatedState).size() + " buildings");
    }

    private GameStateMetadata metadataOf(PlayerGameState state, Instant now) {
        if (state.metadataJson() == null || state.metadataJson().isBlank()) {
            return GameStateMetadata.fromPopulation(state.populationSummary(), OnboardingProgress.defaults(), List.of(), List.of());
        }
        try {
            GameStateMetadata metadata = objectMapper.readValue(state.metadataJson(), GameStateMetadata.class);
            return new GameStateMetadata(
                    metadata.citizenMetaData(),
                    metadata.troopMetaData(),
                    metadata.agingState() == null ? AgingState.defaults(now) : metadata.agingState(),
                    metadata.jobCounts(),
                    metadata.onboardingProgress(),
                    metadata.accountProgression(),
                    metadata.debugModeState(),
                    metadata.resourceNodes(),
                    metadata.castleBuildings(),
                    metadata.interiorInstanceIndex()
            );
        } catch (Exception ex) {
            LOGGER.warning(() -> "Failed to decode building metadata. Falling back to empty buildings. " + ex.getMessage());
            return GameStateMetadata.fromPopulation(state.populationSummary(), OnboardingProgress.defaults(), List.of(), List.of());
        }
    }

    private PlayerGameState rewriteBuildings(PlayerGameState state, List<CastleBuildingData> buildings, Instant now) {
        GameStateMetadata metadata = metadataOf(state, now);
        GameStateMetadata updatedMetadata = new GameStateMetadata(
                metadata.citizenMetaData(),
                metadata.troopMetaData(),
                metadata.agingState(),
                metadata.jobCounts(),
                metadata.onboardingProgress(),
                metadata.accountProgression(),
                metadata.debugModeState(),
                metadata.resourceNodes(),
                buildings,
                metadata.interiorInstanceIndex()
        );
        try {
            return state.withMetadataJson(objectMapper.writeValueAsString(updatedMetadata), now);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to encode building metadata", ex);
        }
    }

    private Vector3d areaOrigin(UUID playerId, PlayerGameState state, BuildingAreaType areaType) {
        if (areaType == BuildingAreaType.CASTLE_SURFACE) {
            if (state.castleLocation() == null) {
                return new Vector3d(0.0D, 0.0D, 0.0D);
            }
            return new Vector3d(state.castleLocation().x(), state.castleLocation().y(), state.castleLocation().z());
        }
        int interiorIndex = gameStateService.interiorInstanceIndex(state);
        return interiorLayoutService.createLayoutForCastle(state.castleLocation(), interiorIndex).origin();
    }

    private String areaWorldName(UUID playerId, PlayerGameState state, BuildingAreaType areaType) {
        if (areaType == BuildingAreaType.CASTLE_SURFACE) {
            return state.castleLocation() == null ? null : state.castleLocation().worldName();
        }
        return playerId == null ? null : interiorInstanceService.worldNameFor(playerId);
    }

    private double progressRatio(CastleBuildingData buildingData, Instant now) {
        if (!buildingData.isUnderConstruction()) {
            return 1.0D;
        }
        long totalMillis = Math.max(1L, buildingData.constructionEndsAt().toEpochMilli() - buildingData.constructionStartedAt().toEpochMilli());
        long elapsedMillis = Math.max(0L, now.toEpochMilli() - buildingData.constructionStartedAt().toEpochMilli());
        return Math.max(0.0D, Math.min(1.0D, elapsedMillis / (double) totalMillis));
    }

    private long remainingSeconds(CastleBuildingData buildingData, Instant now) {
        if (!buildingData.isUnderConstruction()) {
            return 0L;
        }
        return Math.max(0L, buildingData.constructionEndsAt().getEpochSecond() - now.getEpochSecond());
    }

    private double distance(Vector3d first, Vector3d second) {
        double dx = first.getX() - second.getX();
        double dy = first.getY() - second.getY();
        double dz = first.getZ() - second.getZ();
        return Math.sqrt((dx * dx) + (dy * dy) + (dz * dz));
    }

    private Instant resolveNow(PlayerGameState state) {
        return state.updatedAt() == null ? Instant.now() : state.updatedAt();
    }

    private String shortId(UUID buildingId) {
        String value = buildingId.toString();
        return value.substring(0, Math.min(8, value.length()));
    }

    private String percent(double value) {
        return (int) Math.round(value * 100.0D) + "%";
    }

    private String promotionDiscountLabel(PromotionCost discount) {
        return discount.foodCost() + "F/" + discount.woodCost() + "W/" + discount.ironCost() + "I";
    }
}
