package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.math.vector.Vector3i;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.world.ProtectedBlockMetadata;
import com.tavall.hytale.resourcegame.world.ProtectedPlacementZone;
import com.tavall.hytale.resourcegame.world.ProtectedStructureType;
import com.tavall.hytale.resourcegame.world.WorldBlockKey;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks runtime protection metadata for structure-owned blocks.
 */
public final class StructureProtectionService implements IDependencyInjectableConcrete {
    private final Map<WorldBlockKey, ProtectedBlockMetadata> protectedBlocks = new ConcurrentHashMap<>();
    private final Map<String, Set<WorldBlockKey>> structureBlocks = new ConcurrentHashMap<>();
    private final Map<String, ProtectedPlacementZone> placementZones = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> placementZonesByWorld = new ConcurrentHashMap<>();

    public void replaceStructure(
            String structureKey,
            UUID ownerId,
            ProtectedStructureType structureType,
            String worldName,
            String assetType,
            Set<Vector3i> blockPositions
    ) {
        if (structureKey == null || ownerId == null || structureType == null || worldName == null) {
            return;
        }
        clearStructure(structureKey);
        if (blockPositions == null || blockPositions.isEmpty()) {
            return;
        }
        Set<WorldBlockKey> keys = new HashSet<>();
        ProtectedBlockMetadata metadata = new ProtectedBlockMetadata(ownerId, structureKey, structureType, assetType);
        for (Vector3i blockPosition : blockPositions) {
            if (blockPosition == null) {
                continue;
            }
            WorldBlockKey key = WorldBlockKey.of(worldName, blockPosition);
            keys.add(key);
            protectedBlocks.put(key, metadata);
        }
        if (!keys.isEmpty()) {
            structureBlocks.put(structureKey, Set.copyOf(keys));
        }
    }

    public void replacePlacementZone(
            String structureKey,
            ProtectedStructureType structureType,
            String worldName,
            Vector3i centerBlock,
            int radiusBlocks
    ) {
        if (structureKey == null || structureKey.isBlank() || structureType == null || worldName == null || centerBlock == null) {
            return;
        }
        clearPlacementZone(structureKey);
        if (radiusBlocks <= 0) {
            return;
        }
        ProtectedPlacementZone zone = new ProtectedPlacementZone(
                structureKey,
                structureType,
                worldName,
                centerBlock.getX(),
                centerBlock.getY(),
                centerBlock.getZ(),
                radiusBlocks
        );
        placementZones.put(structureKey, zone);
        placementZonesByWorld.compute(worldName, (key, existing) -> {
            Set<String> updated = existing == null ? new HashSet<>() : new HashSet<>(existing);
            updated.add(structureKey);
            return Set.copyOf(updated);
        });
    }

    public void clearStructure(String structureKey) {
        if (structureKey == null || structureKey.isBlank()) {
            return;
        }
        Set<WorldBlockKey> keys = structureBlocks.remove(structureKey);
        if (keys == null) {
            return;
        }
        for (WorldBlockKey key : keys) {
            protectedBlocks.remove(key);
        }
        clearPlacementZone(structureKey);
    }

    public boolean isProtected(String worldName, Vector3i blockPosition) {
        return metadata(worldName, blockPosition).isPresent();
    }

    public boolean isPlacementRestricted(String worldName, Vector3i blockPosition) {
        if (isProtected(worldName, blockPosition)) {
            return true;
        }
        if (worldName == null || blockPosition == null) {
            return false;
        }
        Set<String> zoneKeys = placementZonesByWorld.get(worldName);
        if (zoneKeys == null || zoneKeys.isEmpty()) {
            return false;
        }
        for (String structureKey : zoneKeys) {
            ProtectedPlacementZone zone = placementZones.get(structureKey);
            if (zone != null && zone.contains(blockPosition)) {
                return true;
            }
        }
        return false;
    }

    public Optional<ProtectedBlockMetadata> metadata(String worldName, Vector3i blockPosition) {
        if (worldName == null || blockPosition == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(protectedBlocks.get(WorldBlockKey.of(worldName, blockPosition)));
    }

    private void clearPlacementZone(String structureKey) {
        ProtectedPlacementZone removed = placementZones.remove(structureKey);
        if (removed == null) {
            return;
        }
        placementZonesByWorld.computeIfPresent(removed.worldName(), (world, existing) -> {
            if (existing == null || existing.isEmpty() || !existing.contains(structureKey)) {
                return existing;
            }
            Set<String> updated = new HashSet<>(existing);
            updated.remove(structureKey);
            return updated.isEmpty() ? null : Set.copyOf(updated);
        });
    }
}
