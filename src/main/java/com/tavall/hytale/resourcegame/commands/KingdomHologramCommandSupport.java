package com.tavall.hytale.resourcegame.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.services.WorldLabelService;
import com.tavall.hytale.resourcegame.tasks.WorldTasks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Debug command helpers for spawning and clearing hologram-style world labels.
 */
public final class KingdomHologramCommandSupport implements IDependencyInjectableConcrete {
    private final WorldLabelService worldLabelService;
    private final Map<UUID, List<Ref<EntityStore>>> hologramRefs = new ConcurrentHashMap<>();

    public KingdomHologramCommandSupport(WorldLabelService worldLabelService) {
        this.worldLabelService = Objects.requireNonNull(worldLabelService, "worldLabelService");
    }

    public void handle(CommandContext context, Player player, List<String> tokens) {
        if (tokens.size() < 2) {
            context.sendMessage(Message.raw("Usage: /kd hologram spawn <text> | stack <line1|line2|...> | clear").color("yellow"));
            return;
        }
        String action = tokens.get(1).toLowerCase(java.util.Locale.ROOT);
        switch (action) {
            case "spawn" -> handleSpawn(context, player, tokens);
            case "stack" -> handleStack(context, player, tokens);
            case "clear" -> handleClear(context, player);
            default -> context.sendMessage(Message.raw("Unknown hologram action.").color("red"));
        }
    }

    private void handleSpawn(CommandContext context, Player player, List<String> tokens) {
        if (tokens.size() < 3) {
            context.sendMessage(Message.raw("Usage: /kd hologram spawn <text>").color("yellow"));
            return;
        }
        String text = String.join(" ", tokens.subList(2, tokens.size()));
        spawnLabels(player, List.of(text));
        context.sendMessage(Message.raw("Hologram spawned.").color("green"));
    }

    private void handleStack(CommandContext context, Player player, List<String> tokens) {
        if (tokens.size() < 3) {
            context.sendMessage(Message.raw("Usage: /kd hologram stack <line1|line2|...>").color("yellow"));
            return;
        }
        String joined = String.join(" ", tokens.subList(2, tokens.size()));
        String[] parts = joined.split("\\\\|");
        List<String> lines = new ArrayList<>();
        for (String part : parts) {
            if (part != null && !part.isBlank()) {
                lines.add(part.trim());
            }
        }
        if (lines.isEmpty()) {
            context.sendMessage(Message.raw("No text lines provided.").color("red"));
            return;
        }
        spawnLabels(player, lines);
        context.sendMessage(Message.raw("Hologram stack spawned.").color("green"));
    }

    private void handleClear(CommandContext context, Player player) {
        clear(player.getUuid());
        context.sendMessage(Message.raw("Holograms cleared.").color("green"));
    }

    private void spawnLabels(Player player, List<String> lines) {
        if (player == null || player.getWorld() == null || player.getTransformComponent() == null) {
            return;
        }
        UUID playerId = player.getUuid();
        Vector3d base = player.getTransformComponent().getPosition();
        if (base == null) {
            return;
        }
        Vector3d topPosition = base.add(0.0D, 2.6D, 0.0D);
        WorldTasks.executeSafe(player.getWorld(), "KingdomHologramCommandSupport.spawnLabels", () -> {
            clear(playerId);
            List<Ref<EntityStore>> refs = worldLabelService.spawnLabelStack(player.getWorld(), topPosition, lines);
            hologramRefs.put(playerId, refs);
        });
    }

    private void clear(UUID playerId) {
        List<Ref<EntityStore>> refs = hologramRefs.remove(playerId);
        if (refs == null) {
            return;
        }
        refs.forEach(this::removeSafely);
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
                WorldTasks.executeSafe(world, "KingdomHologramCommandSupport.removeSafely", remove);
                return;
            }
        }
        remove.run();
    }
}
