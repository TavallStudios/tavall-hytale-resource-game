package com.tavall.hytale.resourcegame.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Generic UI action event payload.
 */
public final class UiActionEventData {
    public static final String KEY_ACTION = "Action";
    public static final String KEY_PAYLOAD = "Payload";

    public static final BuilderCodec<UiActionEventData> CODEC = BuilderCodec.builder(
            UiActionEventData.class,
            UiActionEventData::new
    )
            .append(new KeyedCodec<>(KEY_ACTION, Codec.STRING), (value, field) -> value.action = field, value -> value.action)
            .add()
            .append(new KeyedCodec<>(KEY_PAYLOAD, Codec.STRING), (value, field) -> value.payload = field, value -> value.payload)
            .add()
            .build();

    private String action;
    private String payload;

    public UiActionEventData() {
    }

    public String action() {
        return action;
    }

    public String payload() {
        return payload;
    }

    public static UiActionEventData action(String action) {
        UiActionEventData eventData = new UiActionEventData();
        eventData.action = action;
        return eventData;
    }

    public static UiActionEventData actionWithPayload(String action, String payload) {
        UiActionEventData eventData = new UiActionEventData();
        eventData.action = action;
        eventData.payload = payload;
        return eventData;
    }
}
