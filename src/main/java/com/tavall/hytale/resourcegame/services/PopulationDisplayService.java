package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.Frozen;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.modules.entity.component.AudioComponent;
import com.hypixel.hytale.server.core.modules.entity.component.Intangible;
import com.hypixel.hytale.server.core.modules.entity.component.Invulnerable;
import com.hypixel.hytale.server.core.modules.entity.component.MovementAudioComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.tavall.hytale.resourcegame.config.PopulationDisplayConfig;
import com.tavall.hytale.resourcegame.domain.CitizenJobType;
import com.tavall.hytale.resourcegame.interior.InteriorLayout;
import com.tavall.hytale.resourcegame.population.PopulationDisplayRefs;
import com.tavall.hytale.resourcegame.domain.PopulationSummary;
import it.unimi.dsi.fastutil.Pair;

import java.util.EnumMap;
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
    private final Map<UUID, PopulationDisplayRefs> displayRefs = new ConcurrentHashMap<>();

    public PopulationDisplayService(PopulationDisplayConfig displayConfig) {
        this.displayConfig = displayConfig;
    }

    @Override
    public void ensureDisplays(UUID playerId, World world, InteriorLayout layout, PopulationSummary summary) {
        PopulationDisplayRefs existing = displayRefs.get(playerId);
        if (existing != null && hasCompleteAnchors(existing, layout)) {
            updateDisplays(playerId, summary);
            return;
        }
        Store<EntityStore> store = world.getEntityStore().getStore();
        EnumMap<CitizenJobType, Ref<EntityStore>> workerRefs = new EnumMap<>(CitizenJobType.class);
        for (CitizenJobType jobType : CitizenJobType.values()) {
            Vector3d position = layout.workerAnchors().get(jobType);
            if (position == null) {
                continue;
            }
            workerRefs.put(jobType, spawnAnchor(store, position, workerDisplayLabel(summary, jobType)));
        }
        Ref<EntityStore> troops = workerRefs.get(CitizenJobType.SOLDIER);
        displayRefs.put(playerId, new PopulationDisplayRefs(workerRefs, troops));
        LOGGER.info(() -> String.format(
                "Population displays ready for %s in world %s. citizens=%s troops=%s",
                playerId,
                world.getName(),
                summary.citizenCount(),
                summary.troopCount()
        ));
    }

    @Override
    public void updateDisplays(UUID playerId, PopulationSummary summary) {
        PopulationDisplayRefs refs = displayRefs.get(playerId);
        if (refs == null) {
            return;
        }
        for (Map.Entry<CitizenJobType, Ref<EntityStore>> entry : refs.workerRefs().entrySet()) {
            updateDisplay(entry.getValue(), workerDisplayLabel(summary, entry.getKey()));
        }
        updateDisplay(refs.troopsRef(), troopDisplayLabel(summary));
        LOGGER.info(() -> String.format(
                "Population displays updated for %s. citizens=%s troops=%s",
                playerId,
                summary.citizenCount(),
                summary.troopCount()
        ));
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

    private Ref<EntityStore> spawnAnchor(Store<EntityStore> store, Vector3d position, String label) {
        NPCPlugin npcPlugin = NPCPlugin.get();
        int roleIndex = new NpcRoleResolver().resolveRoleIndex(displayConfig.npcRoleName());
        if (roleIndex < 0) {
            return null;
        }
        Pair<Ref<EntityStore>, NPCEntity> pair = npcPlugin.spawnEntity(store, roleIndex, position, Vector3f.ZERO, null, null);
        Ref<EntityStore> ref = pair.first();
        if (ref != null && ref.isValid()) {
            store.putComponent(ref, DisplayNameComponent.getComponentType(), new DisplayNameComponent(Message.raw(label)));
            store.putComponent(ref, Nameplate.getComponentType(), new Nameplate(label));
            store.ensureComponent(ref, Frozen.getComponentType());
            store.ensureComponent(ref, Intangible.getComponentType());
            store.ensureComponent(ref, Invulnerable.getComponentType());
            silenceAnchor(store, ref);
        }
        return ref;
    }

    private void updateDisplay(Ref<EntityStore> ref, String label) {
        if (ref == null || !ref.isValid()) {
            return;
        }
        Store<EntityStore> store = ref.getStore();
        store.putComponent(ref, DisplayNameComponent.getComponentType(), new DisplayNameComponent(Message.raw(label)));
        store.putComponent(ref, Nameplate.getComponentType(), new Nameplate(label));
        store.ensureComponent(ref, Frozen.getComponentType());
        store.ensureComponent(ref, Intangible.getComponentType());
        store.ensureComponent(ref, Invulnerable.getComponentType());
        silenceAnchor(store, ref);
    }

    private void silenceAnchor(Store<EntityStore> store, Ref<EntityStore> ref) {
        store.removeComponent(ref, AudioComponent.getComponentType());
        store.removeComponent(ref, MovementAudioComponent.getComponentType());
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
}
