package com.tavall.hytale.resourcegame.ui;

import au.ellie.hyui.builders.InterfaceBuilder;
import au.ellie.hyui.builders.UIElementBuilder;
import au.ellie.hyui.html.TemplateProcessor;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * Immutable page data passed from the HyUI builder to page subclasses.
 */
public record HyUiPageDefinition(
        String uiFile,
        List<UIElementBuilder<?>> topLevelElements,
        List<BiConsumer<UICommandBuilder, UIEventBuilder>> editCallbacks,
        String templateHtml,
        TemplateProcessor templateProcessor,
        boolean runtimeTemplateUpdatesEnabled,
        InterfaceBuilder<?> rootElementBuilder
) {
}
