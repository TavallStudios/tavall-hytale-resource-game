package com.tavall.hytale.resourcegame.ui;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hypixel.hytale.common.semver.SemverRange;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class CustomUiDocumentRegressionTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Path RESOURCE_ROOT = Path.of("src", "main", "resources");
    private static final Path UI_ROOT = RESOURCE_ROOT.resolve(Path.of("Common", "UI", "Custom", "Pages"));
    private static final Path MANIFEST_PATH = RESOURCE_ROOT.resolve("manifest.json");
    private static final List<String> UNSAFE_CUSTOM_UI_TOKENS = List.of(
            "BackgroundColorHover",
            "BackgroundColorPressed",
            "TextColorHover"
    );

    @Test
    void uiPagesAreStandaloneDocumentsWithNoBomOrSharedImports() throws IOException {
        assertTrue(Files.isDirectory(UI_ROOT), "UI page directory is missing");
        List<Path> pages = Files.list(UI_ROOT)
                .filter(path -> path.getFileName().toString().endsWith(".html"))
                .sorted()
                .toList();
        assertFalse(pages.isEmpty(), "No HyUI pages were found");

        for (Path page : pages) {
            String content = Files.readString(page, StandardCharsets.UTF_8);
            assertFalse(content.isBlank(), () -> "UI page is empty: " + page);
            assertFalse(content.startsWith("\uFEFF"), () -> "UI page has UTF-8 BOM: " + page);
            assertFalse(content.contains("../Common.ui"), () -> "UI page still imports Common.ui: " + page);
            assertTrue(content.contains("<style>"), () -> "HyUI page must contain inline styles: " + page);
            assertTrue(content.contains("id=\"ResourceGamePageRoot\""), () -> "HyUI page must contain the Resource Game page root: " + page);
            assertFalse(content.contains("src=\"../Textures/ResourceGame/"), () -> "HyUI img src must not use native .ui-relative texture paths: " + page);
            assertFalse(content.contains("<script"), () -> "HyUI page must not use JavaScript: " + page);
            for (String unsafeToken : UNSAFE_CUSTOM_UI_TOKENS) {
                assertFalse(content.contains(unsafeToken), () -> "UI page uses unsupported token " + unsafeToken + ": " + page);
            }
        }
    }

    @Test
    void manifestKeepsAssetPackEnabled() throws IOException {
        String manifest = Files.readString(MANIFEST_PATH, StandardCharsets.UTF_8);
        JsonNode manifestNode = OBJECT_MAPPER.readTree(manifest);
        String hyUiRange = manifestNode.path("Dependencies").path("Ellie:HyUI").asText();
        assertTrue(manifest.contains("\"IncludesAssetPack\": true"), "manifest.json must keep IncludesAssetPack enabled");
        assertTrue(manifest.contains("\"Ellie:HyUI\": \"=0.9.5\""), "manifest.json must require HyUI 0.9.5");
        SemverRange.fromString(hyUiRange);
        assertFalse(Files.exists(RESOURCE_ROOT.resolve(Path.of("Common", "UI", "Custom", "Common.ui"))), "Common.ui should not exist anymore");
    }
}
