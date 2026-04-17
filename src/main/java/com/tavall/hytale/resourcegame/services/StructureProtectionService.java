package com.tavall.hytale.resourcegame.services;

import com.hypixel.hytale.math.vector.Vector3i;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.world.ProtectedBlockMetadata;
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
    }

    public boolean isProtected(String worldName, Vector3i blockPosition) {
        return metadata(worldName, blockPosition).isPresent();
    }

    public Optional<ProtectedBlockMetadata> metadata(String worldName, Vector3i blockPosition) {
        if (worldName == null || blockPosition == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(protectedBlocks.get(WorldBlockKey.of(worldName, blockPosition)));
    }
}
