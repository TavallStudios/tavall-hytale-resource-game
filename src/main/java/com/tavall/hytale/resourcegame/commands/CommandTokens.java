package com.tavall.hytale.resourcegame.commands;

import com.hypixel.hytale.server.core.command.system.CommandContext;

import java.util.Arrays;
import java.util.List;

/**
 * Tokenizes command input strings.
 */
public final class CommandTokens {
    private CommandTokens() {
    }

    public static List<String> tokens(CommandContext context) {
        String input = context.getInputString();
        if (input == null || input.isBlank()) {
            return List.of();
        }
        String[] rawTokens = input.trim().split("\\s+");
        if (rawTokens.length <= 1) {
            return List.of();
        }
        return Arrays.asList(rawTokens).subList(1, rawTokens.length);
    }
}
