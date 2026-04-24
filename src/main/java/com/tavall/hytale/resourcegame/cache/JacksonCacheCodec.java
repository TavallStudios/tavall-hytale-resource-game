package com.tavall.hytale.resourcegame.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.tavall.abstractcache.semantic.spi.CacheCodec;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Jackson-based cache codec for semantic cache payloads.
 */
public final class JacksonCacheCodec<T> implements CacheCodec<T> {
    private final ObjectMapper objectMapper;
    private final Class<T> payloadType;
    private final String codecId;

    public JacksonCacheCodec(ObjectMapper objectMapper, Class<T> payloadType, String codecId) {
        this.objectMapper = objectMapper;
        this.payloadType = payloadType;
        this.codecId = codecId;
    }

    @Override
    public String codecId() {
        return codecId;
    }

    @Override
    public byte[] encode(T value) {
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to encode cache payload", e);
        }
    }

    @Override
    public T decode(byte[] bytes) {
        try {
            return objectMapper.readValue(bytes, payloadType);
        } catch (IOException e) {
            String payload = new String(bytes, StandardCharsets.UTF_8);
            throw new IllegalStateException("Failed to decode cache payload: " + payload, e);
        }
    }
}
