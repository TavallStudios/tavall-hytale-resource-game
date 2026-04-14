package com.tavall.hytale.resourcegame.ui;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class CustomUiDocumentRegressionTest {
    private static final Path RESOURCE_ROOT = Path.of("src", "main", "resources");
    private static final Path UI_ROOT = RESOURCE_ROOT.resolve(Path.of("Common", "UI", "Custom", "Pages"));
    private static final Path MANIFEST_PATH = RESOURCE_ROOT.resolve("manifest.json");

    @Test
    void uiPagesAreStandaloneDocumentsWithNoBomOrSharedImports() throws IOException {
        assertTrue(Files.isDirectory(UI_ROOT), "UI page directory is missing");
        List<Path> pages = Files.list(UI_ROOT)
                .filter(path -> path.getFileName().toString().endsWith(".ui"))
                .sorted()
                .toList();
        assertFalse(pages.isEmpty(), "No custom UI pages were found");

        for (Path page : pages) {
            String content = Files.readString(page, StandardCharsets.UTF_8);
            assertFalse(content.isBlank(), () -> "UI page is empty: " + page);
            assertFalse(content.startsWith("\uFEFF"), () -> "UI page has UTF-8 BOM: " + page);
            assertFalse(content.contains("../Common.ui"), () -> "UI page still imports Common.ui: " + page);
            assertTrue(content.stripLeading().startsWith("Group"), () -> "UI page must start with a root Group: " + page);
        }
    }

    @Test
    void manifestKeepsAssetPackEnabled() throws IOException {
        String manifest = Files.readString(MANIFEST_PATH, StandardCharsets.UTF_8);
        assertTrue(manifest.contains("\"IncludesAssetPack\": true"), "manifest.json must keep IncludesAssetPack enabled");
        assertFalse(Files.exists(RESOURCE_ROOT.resolve(Path.of("Common", "UI", "Custom", "Common.ui"))), "Common.ui should not exist anymore");
    }
}
