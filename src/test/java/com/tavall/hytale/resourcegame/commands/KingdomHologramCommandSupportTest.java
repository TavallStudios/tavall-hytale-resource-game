package com.tavall.hytale.resourcegame.commands;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class KingdomHologramCommandSupportTest {
    @Test
    void stackLineParserSplitsPipeSeparatedHologramLines() {
        List<String> lines = KingdomHologramCommandSupport.parseStackLines("Test hologram|Second line|  Third line  ");

        assertEquals(List.of("Test hologram", "Second line", "Third line"), lines);
    }

    @Test
    void stackLineParserDropsBlankLines() {
        List<String> lines = KingdomHologramCommandSupport.parseStackLines("Top||  |Bottom");

        assertEquals(List.of("Top", "Bottom"), lines);
    }
}
