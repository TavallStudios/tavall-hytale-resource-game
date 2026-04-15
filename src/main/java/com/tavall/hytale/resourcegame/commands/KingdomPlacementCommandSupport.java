package com.tavall.hytale.resourcegame.commands;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.dependency.interfaces.IPlacementModeService;
import com.tavall.hytale.resourcegame.domain.PlacementRequest;
import com.tavall.hytale.resourcegame.domain.PlacementResult;
import com.tavall.hytale.resourcegame.resources.ResourceType;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Handles `/kd place ...` command flows.
 */
public final class KingdomPlacementCommandSupport implements IDependencyInjectableConcrete {
    private final IPlacementModeService placementModeService;

    public KingdomPlacementCommandSupport(IPlacementModeService placementModeService) {
        this.placementModeService = Objects.requireNonNull(placementModeService, "placementModeService");
    }

    public void handle(CommandContext context, Player player, List<String> tokens) {
        if (tokens.size() < 2) {
            context.sendMessage(Message.raw("Usage: /kd place castle|node <type>|confirm [here]|cancel|status|preview").color("yellow"));
            return;
        }
        String action = tokens.get(1).toLowerCase(Locale.ROOT);
        switch (action) {
            case "castle" -> armCastlePlacement(context, player);
            case "node" -> armNodePlacement(context, player, tokens);
            case "confirm" -> sendResult(context, confirmPlacement(context, player, tokens));
            case "cancel" -> sendResult(context, placementModeService.cancelPlacement(player.getUuid()));
            case "status" -> sendStatus(context, player);
            case "preview" -> sendResult(context, placementModeService.refreshPreview(player));
            default -> context.sendMessage(Message.raw("Unknown place action.").color("red"));
        }
    }

    private void armCastlePlacement(CommandContext context, Player player) {
        PlacementRequest request = placementModeService.armCastlePlacement(player);
        context.sendMessage(Message.raw(request.summary() + " armed. Click the ground or use /kd place confirm.").color("green"));
    }

    private void armNodePlacement(CommandContext context, Player player, List<String> tokens) {
        if (tokens.size() < 3) {
            context.sendMessage(Message.raw("Usage: /kd place node <food|wood|iron>").color("yellow"));
            return;
        }
        ResourceType resourceType = parseResource(tokens.get(2));
        if (resourceType == null) {
            context.sendMessage(Message.raw("Unknown resource type.").color("red"));
            return;
        }
        PlacementRequest request = placementModeService.armNodePlacement(player, resourceType);
        context.sendMessage(Message.raw(request.summary() + " armed. Click the ground or use /kd place confirm.").color("green"));
    }

    private void sendStatus(CommandContext context, Player player) {
        Optional<PlacementRequest> request = placementModeService.activePlacement(player.getUuid());
        if (request.isEmpty()) {
            context.sendMessage(Message.raw("No active placement.").color("yellow"));
            return;
        }
        context.sendMessage(Message.raw(request.get().summary() + " active in " + request.get().armedWorldName() + ".").color("yellow"));
    }

    private void sendResult(CommandContext context, PlacementResult result) {
        if (!result.handled()) {
            context.sendMessage(Message.raw("No active placement.").color("yellow"));
            return;
        }
        context.sendMessage(Message.raw(result.message()).color(result.success() ? "green" : "red"));
    }

    private ResourceType parseResource(String token) {
        return switch (token.toLowerCase(Locale.ROOT)) {
            case "food" -> ResourceType.FOOD;
            case "wood" -> ResourceType.WOOD;
            case "iron" -> ResourceType.IRON;
            default -> null;
        };
    }

    private PlacementResult confirmPlacement(CommandContext context, Player player, List<String> tokens) {
        if (tokens.size() >= 3 && "here".equalsIgnoreCase(tokens.get(2))) {
            Vector3i currentBlock = currentStandingBlock(player);
            if (currentBlock == null) {
                context.sendMessage(Message.raw("Unable to resolve current standing block.").color("red"));
                return PlacementResult.failure("Unable to resolve current standing block.");
            }
            return placementModeService.confirmPlacement(player, currentBlock);
        }
        return placementModeService.confirmPlacementFromAim(player);
    }

    private Vector3i currentStandingBlock(Player player) {
        TransformComponent transform = player.getTransformComponent();
        if (transform == null) {
            return null;
        }
        Vector3d position = transform.getPosition();
        if (position == null) {
            return null;
        }
        return new Vector3i(
                (int) Math.floor(position.getX()),
                (int) Math.floor(position.getY()) - 1,
                (int) Math.floor(position.getZ())
        );
    }
}
