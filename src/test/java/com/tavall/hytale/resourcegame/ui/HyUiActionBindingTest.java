package com.tavall.hytale.resourcegame.ui;

import au.ellie.hyui.events.DynamicPageData;
import au.ellie.hyui.events.UIEventActions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public final class HyUiActionBindingTest {
    @Test
    void normalizesBotSelectorsToHyUiElementIds() {
        HyUiActionBinding binding = HyUiActionBinding.action("#EnterInteriorButton.Text", UiActions.ENTER_INTERIOR);

        assertEquals("EnterInteriorButton", binding.elementId());
        assertEquals(UiActions.ENTER_INTERIOR, binding.eventData().action());
    }

    @Test
    void preservesCommandPayloadForUiCommandButtons() {
        HyUiActionBinding binding = HyUiActionBinding.command("#StageFarmsteadButton", "/kd buildings stage farmstead");

        assertEquals("StageFarmsteadButton", binding.elementId());
        assertEquals(UiActions.RUN_COMMAND, binding.eventData().action());
        assertEquals("/kd buildings stage farmstead", binding.eventData().payload());
    }

    @Test
    void preservesDirectActionEventsForBotHarnesses() {
        DynamicPageData data = new DynamicPageData();
        data.action = UiActions.OPEN_CASTLE_MAIN;
        data.values.put(UiActionEventData.KEY_PAYLOAD, "castle");

        UiActionEventData eventData = BaseUiPage.directAction(data);

        assertEquals(UiActions.OPEN_CASTLE_MAIN, eventData.action());
        assertEquals("castle", eventData.payload());
    }

    @Test
    void ignoresHyUiButtonEventsAlreadyHandledByCallbacks() {
        DynamicPageData data = new DynamicPageData();
        data.action = UIEventActions.BUTTON_CLICKED;

        assertNull(BaseUiPage.directAction(data));
    }
}
