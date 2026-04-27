package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.tavall.hytale.resourcegame.config.PopulationDisplayConfig;
import com.tavall.hytale.resourcegame.domain.CitizenJobType;
import com.tavall.hytale.resourcegame.interior.InteriorLayout;
import com.tavall.hytale.resourcegame.population.PopulationDisplayRefs;
import com.tavall.hytale.resourcegame.domain.PopulationSummary;
import com.tavall.hytale.resourcegame.tasks.WorldTasks;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Spawns and updates population display entities inside interiors.
 */
public final class PopulationDisplayService implements PopulationDisplayGateway {
    private static final Logger LOGGER = Logger.getLogger(PopulationDisplayService.class.getName());
    private final PopulationDisplayConfig displayConfig;
    private final WorldLabelService worldLabelService;
    private final Map<UUID, PopulationDisplayRefs> displayRefs = new ConcurrentHashMap<>();

    public PopulationDisplayService(PopulationDisplayConfig displayConfig, WorldLabelService worldLabelService) {
        this.displayConfig = displayConfig;
        this.worldLabelService = worldLabelService;
    }

    @Override
    public void ensureDisplays(UUID playerId, World world, InteriorLayout layout, PopulationSummary summary) {
        if (playerId == null || world == null || layout == null || summary == null) {
            return;
        }
        PopulationDisplayRefs existing = displayRefs.get(playerId);
        DisplaySnapshot snapshot = buildSnapshot(layout, summary);
        if (canReuse(existing, world, snapshot)) {
            return;
        }
        clearDisplays(playerId);
        spawnDisplays(playerId, world, snapshot, "ready");
    }

    @Override
    public void updateDisplays(UUID playerId, PopulationSummary summary) {
        if (playerId == null || summary == null) {
            return;
        }
        PopulationDisplayRefs existing = displayRefs.get(playerId);
        if (existing == null) {
            return;
        }
        World world = resolveWorld(existing.worldName());
        if (world == null) {
            clearDisplays(playerId);
            return;
        }
        DisplaySnapshot snapshot = buildSnapshot(existing.workerPositions(), summary);
        if (canReuse(existing, world, snapshot)) {
            return;
        }
        clearDisplays(playerId);
        spawnDisplays(playerId, world, snapshot, "updated");
    }

    private void spawnDisplays(UUID playerId, World world, DisplaySnapshot snapshot, String action) {
        if (playerId == null || world == null || snapshot == null) {
            return;
        }
        EnumMap<CitizenJobType, Ref<EntityStore>> workerRefs = new EnumMap<>(CitizenJobType.class);
        for (Map.Entry<CitizenJobType, Vector3d> entry : snapshot.workerPositions().entrySet()) {
            CitizenJobType jobType = entry.getKey();
            Vector3d position = entry.getValue();
            if (position == null) {
                continue;
            }
            workerRefs.put(jobType, spawnAnchor(world, position, snapshot.workerLabels().get(jobType)));
        }
        Ref<EntityStore> troopsRef = workerRefs.get(CitizenJobType.SOLDIER);
        displayRefs.put(playerId, new PopulationDisplayRefs(
                world.getName(),
                snapshot.workerPositions(),
                snapshot.workerLabels(),
                workerRefs,
                troopsRef,
                snapshot.troopLabel(),
                snapshot.citizenCount(),
                snapshot.troopCount()
        ));
        LOGGER.info(() -> String.format(
                "Population displays %s for %s in world %s. citizens=%s troops=%s",
                action,
                playerId,
                world.getName(),
                snapshot.citizenCount(),
                snapshot.troopCount()
        ));
    }

    @Override
    public void clearDisplays(UUID playerId) {
        PopulationDisplayRefs refs = displayRefs.remove(playerId);
        if (refs == null) {
            return;
        }
        refs.allRefs().forEach(this::removeSafely);
    }

    private void removeSafely(Ref<EntityStore> ref) {
        if (ref == null || !ref.isValid()) {
            return;
        }
        Store<EntityStore> store = ref.getStore();
        Runnable remove = () -> {
            if (ref.isValid()) {
                store.removeEntity(ref, RemoveReason.REMOVE);
            }
        };
        if (store.getExternalData() instanceof EntityStore entityStore) {
            World world = entityStore.getWorld();
            if (world != null) {
                WorldTasks.executeSafe(world, "PopulationDisplayService.removeSafely", remove);
                return;
            }
        }
        remove.run();
    }

    public Optional<CitizenJobType> resolveWorkerType(UUID playerId, Ref<EntityStore> targetRef) {
        PopulationDisplayRefs refs = displayRefs.get(playerId);
        if (refs == null || targetRef == null || !targetRef.isValid()) {
            return Optional.empty();
        }
        return refs.resolveWorkerType(targetRef);
    }

    public boolean isTroopAnchor(UUID playerId, Ref<EntityStore> targetRef) {
        PopulationDisplayRefs refs = displayRefs.get(playerId);
        return refs != null && refs.troopsRef() != null && refs.troopsRef().equals(targetRef);
    }

    private boolean hasCompleteAnchors(PopulationDisplayRefs refs, InteriorLayout layout) {
        if (refs.troopsRef() == null || !refs.troopsRef().isValid()) {
            return false;
        }
        for (CitizenJobType jobType : layout.workerAnchors().keySet()) {
            Ref<EntityStore> ref = refs.workerRefs().get(jobType);
            if (ref == null || !ref.isValid()) {
                return false;
            }
        }
        return true;
    }

    private boolean canReuse(PopulationDisplayRefs refs, World world, DisplaySnapshot snapshot) {
        if (refs == null || world == null || snapshot == null) {
            return false;
        }
        if (!world.getName().equals(refs.worldName())) {
            return false;
        }
        if (!snapshot.workerLabels().equals(refs.workerLabels())) {
            return false;
        }
        if (!snapshot.troopLabel().equals(refs.troopLabel())) {
            return false;
        }
        if (snapshot.citizenCount() != refs.citizenCount() || snapshot.troopCount() != refs.troopCount()) {
            return false;
        }
        return hasCompleteAnchors(refs, snapshot.workerPositions().keySet());
    }

    private boolean hasCompleteAnchors(PopulationDisplayRefs refs, Iterable<CitizenJobType> requiredJobTypes) {
        if (refs == null || refs.troopsRef() == null || !refs.troopsRef().isValid()) {
            return false;
        }
        for (CitizenJobType jobType : requiredJobTypes) {
            Ref<EntityStore> ref = refs.workerRefs().get(jobType);
            if (ref == null || !ref.isValid()) {
                return false;
            }
        }
        return true;
    }

    private DisplaySnapshot buildSnapshot(InteriorLayout layout, PopulationSummary summary) {
        return buildSnapshot(layout.workerAnchors(), summary);
    }

    private DisplaySnapshot buildSnapshot(Map<CitizenJobType, Vector3d> workerPositions, PopulationSummary summary) {
        EnumMap<CitizenJobType, Vector3d> positions = new EnumMap<>(CitizenJobType.class);
        if (workerPositions != null) {
            positions.putAll(workerPositions);
        }
        EnumMap<CitizenJobType, String> labels = new EnumMap<>(CitizenJobType.class);
        for (Map.Entry<CitizenJobType, Vector3d> entry : positions.entrySet()) {
            labels.put(entry.getKey(), workerDisplayLabel(summary, entry.getKey()));
        }
        return new DisplaySnapshot(
                positions,
                labels,
                troopDisplayLabel(summary),
                summary.citizenCount(),
                summary.troopCount()
        );
    }

    private World resolveWorld(String worldName) {
        if (worldName == null || worldName.isBlank()) {
            return null;
        }
        return com.hypixel.hytale.server.core.universe.Universe.get().getWorld(worldName);
    }

    private int workerCount(PopulationSummary summary, CitizenJobType jobType) {
        if (jobType == CitizenJobType.SOLDIER) {
            return Math.max(summary.troopCount(), summary.citizenMetaData().jobCounts().getOrDefault(jobType, 0));
        }
        if (jobType == CitizenJobType.IDLE) {
            int explicitIdle = summary.citizenMetaData().jobCounts().getOrDefault(CitizenJobType.IDLE, -1);
            if (explicitIdle >= 0) {
                return explicitIdle;
            }
            int assigned = summary.citizenMetaData().jobCounts().entrySet().stream()
                    .filter(entry -> entry.getKey() != CitizenJobType.SOLDIER)
                    .mapToInt(Map.Entry::getValue)
                    .sum();
            return Math.max(0, summary.citizenCount() - assigned);
        }
        return Math.max(0, summary.citizenMetaData().jobCounts().getOrDefault(jobType, 0));
    }

    private String workerLabel(CitizenJobType jobType) {
        return switch (jobType) {
            case IDLE -> "Idle Citizens";
            case GATHERER -> "Gatherers";
            case HUNTER -> "Hunters";
            case COOK -> "Cooks";
            case MINER -> "Miners";
            case BLACKSMITH -> "Blacksmith Builders";
            case ARCHITECT -> "Architecture Builders";
            case GRUNT_BUILDER -> "Grunt Builders";
            case BUILDER -> "Legacy Builders";
            case TRAINEE -> "Trainees";
            case SOLDIER -> "Soldiers";
        };
    }

    private Ref<EntityStore> spawnAnchor(World world, Vector3d position, String label) {
        if (world == null || position == null || label == null || label.isBlank() || worldLabelService == null) {
            return null;
        }
        Vector3d labelPosition = new Vector3d(position.getX(), position.getY() + 1.15D, position.getZ());
        return worldLabelService.spawnLabel(world, labelPosition, label);
    }

    private String workerDisplayLabel(PopulationSummary summary, CitizenJobType jobType) {
        int count = workerCount(summary, jobType);
        int productivity = percent(summary.citizenMetaData().productivityMedian());
        int morale = percent(summary.citizenMetaData().moraleMedian());
        int readiness = percent(summary.citizenMetaData().battleReadinessMedian());
        return switch (jobType) {
            case IDLE -> workerLabel(jobType) + " x" + count + " | Mor " + morale + "%";
            case GATHERER, HUNTER, COOK, MINER -> workerLabel(jobType) + " x" + count + " | Out " + productivity + "% | Mor " + morale + "%";
            case BLACKSMITH, ARCHITECT, GRUNT_BUILDER, BUILDER -> workerLabel(jobType) + " x" + count + " | Build " + productivity + "% | Ready " + readiness + "%";
            case TRAINEE -> workerLabel(jobType) + " x" + count + " | Drill " + readiness + "% | Mor " + morale + "%";
            case SOLDIER -> workerLabel(jobType) + " x" + count + " | Might " + summary.might() + " | Ready " + readiness + "%";
        };
    }

    private String troopDisplayLabel(PopulationSummary summary) {
        return displayConfig.troopLabel()
                + " x" + summary.troopCount()
                + " | Might " + summary.might()
                + " | Ready " + percent(summary.citizenMetaData().battleReadinessMedian()) + "%";
    }

    private int percent(double ratio) {
        return (int) Math.round(Math.max(0.0D, Math.min(1.0D, ratio)) * 100.0D);
    }

    private record DisplaySnapshot(
            Map<CitizenJobType, Vector3d> workerPositions,
            Map<CitizenJobType, String> workerLabels,
            String troopLabel,
            int citizenCount,
            int troopCount
    ) {
        private DisplaySnapshot {
            workerPositions = Map.copyOf(new LinkedHashMap<>(workerPositions));
            workerLabels = Map.copyOf(new LinkedHashMap<>(workerLabels));
        }
    }
}
