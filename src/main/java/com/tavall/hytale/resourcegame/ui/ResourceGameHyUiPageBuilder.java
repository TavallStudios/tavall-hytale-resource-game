package com.tavall.hytale.resourcegame.ui;

import au.ellie.hyui.builders.PageBuilder;
import au.ellie.hyui.events.UIContext;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Creates HyUI page definitions while preserving Resource Game page class names.
 */
public final class ResourceGameHyUiPageBuilder extends PageBuilder {
    public static HyUiPageDefinition build(
            String resourcePath,
            Map<String, ?> templateVariables,
            Collection<HyUiActionBinding> actionBindings,
            BiConsumer<UiActionEventData, UIContext> actionHandler
    ) {
        ResourceGameHyUiPageBuilder builder = new ResourceGameHyUiPageBuilder();
        builder.loadHtml(Objects.requireNonNull(resourcePath, "resourcePath"), templateVariables == null ? Map.of() : templateVariables);
        for (HyUiActionBinding binding : actionBindings == null ? List.<HyUiActionBinding>of() : actionBindings) {
            builder.addEventListener(
                    binding.elementId(),
                    CustomUIEventBindingType.Activating,
                    Void.class,
                    (ignored, uiContext) -> actionHandler.accept(binding.eventData(), uiContext)
            );
        }
        return builder.definition();
    }

    private HyUiPageDefinition definition() {
        return new HyUiPageDefinition(
                uiFile,
                List.copyOf(getTopLevelElements()),
                List.copyOf(editCallbacks),
                templateHtml,
                templateProcessor,
                runtimeTemplateUpdatesEnabled,
                this
        );
    }
}
