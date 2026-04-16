package com.tavall.hytale.resourcegame.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.tavall.hytale.resourcegame.dependency.interfaces.IFocusedWorldInteractionService;
import com.tavall.hytale.resourcegame.domain.FocusedWorldTarget;
import com.tavall.hytale.resourcegame.domain.FocusedWorldTargetType;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Handles explicit focus and interaction commands that mirror player world selection flow.
 */
public final class KingdomInteractionCommandSupport implements IDependencyInjectableConcrete {
    private final IFocusedWorldInteractionService focusedWorldInteractionService;

    public KingdomInteractionCommandSupport(IFocusedWorldInteractionService focusedWorldInteractionService) {
        this.focusedWorldInteractionService = Objects.requireNonNull(focusedWorldInteractionService, "focusedWorldInteractionService");
    }

    public void handleFocus(CommandContext context, Player player) {
        Optional<FocusedWorldTarget> target = focusedWorldInteractionService.resolve(player);
        if (target.isEmpty()) {
            context.sendMessage(Message.raw("Focus: none").color("yellow"));
            return;
        }
        context.sendMessage(Message.raw("Focus: " + describe(target.get())).color("green"));
    }

    public void handleInteract(CommandContext context, Player player) {
        Optional<FocusedWorldTarget> target = focusedWorldInteractionService.interact(player);
        if (target.isEmpty()) {
            context.sendMessage(Message.raw("No focused castle, node, or building in front of you.").color("red"));
            return;
        }
        context.sendMessage(Message.raw("Interacted with " + describe(target.get()) + ".").color("green"));
    }

    public void handleScan(CommandContext context, Player player) {
        TransformComponent transform = player.getTransformComponent();
        if (player.getWorld() != null && transform != null) {
            context.sendMessage(Message.raw(
                    "World: "
                            + player.getWorld().getName()
                            + " | pos "
                            + String.format(Locale.ROOT, "%.1f %.1f %.1f", transform.getPosition().getX(), transform.getPosition().getY(), transform.getPosition().getZ())
                            + " | rot "
                            + String.format(Locale.ROOT, "%.1f %.1f %.1f", transform.getRotation().getX(), transform.getRotation().getY(), transform.getRotation().getZ())
            ).color("yellow"));
        }
        handleFocus(context, player);
    }

    private String describe(FocusedWorldTarget target) {
        String base = target.type() == FocusedWorldTargetType.CASTLE
                ? "castle"
                : target.label().toLowerCase(Locale.ROOT);
        return base
                + " | distance "
                + String.format(Locale.ROOT, "%.1f", target.distance())
                + " | alignment "
                + String.format(Locale.ROOT, "%.2f", target.alignmentScore());
    }
}
