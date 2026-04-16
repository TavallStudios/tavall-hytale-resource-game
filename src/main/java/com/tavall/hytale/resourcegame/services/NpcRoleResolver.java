package com.tavall.hytale.resourcegame.services;

import com.tavall.hytale.resourcegame.dependency.IDependencyInjectableConcrete;
import com.hypixel.hytale.server.npc.NPCPlugin;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Resolves placeholder NPC role names with a small fallback chain so prototype visuals survive
 * runtime differences between server builds.
 */
public final class NpcRoleResolver implements IDependencyInjectableConcrete {
    private static final List<String> FALLBACK_ROLE_NAMES = List.of(
            "Kweebec_Seedling",
            "Kweebec_Elder",
            "Goblin_Miner",
            "Goblin_Scrapper",
            "Feran_Civilian",
            "Outlander_Peon",
            "Trork_Guard",
            "kweebec_seedling",
            "kweebec_elder",
            "goblin_miner",
            "goblin_scrapper",
            "feran_civilian",
            "outlander_peon",
            "trork_guard"
    );

    public int resolveRoleIndex(String preferredRoleName) {
        NPCPlugin npcPlugin = NPCPlugin.get();
        for (String candidate : candidates(preferredRoleName)) {
            int roleIndex = npcPlugin.getIndex(candidate);
            if (roleIndex >= 0) {
                return roleIndex;
            }
        }
        return -1;
    }

    private Set<String> candidates(String preferredRoleName) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        if (preferredRoleName != null && !preferredRoleName.isBlank()) {
            values.add(preferredRoleName);
            values.add(preferredRoleName.trim().toLowerCase(Locale.ROOT));
            values.add(titleCase(preferredRoleName));
        }
        values.addAll(FALLBACK_ROLE_NAMES);
        return values;
    }

    private String titleCase(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.substring(0, 1).toUpperCase(Locale.ROOT) + normalized.substring(1);
    }
}
