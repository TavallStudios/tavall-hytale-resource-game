package org.tavall.hytale.resourcegame.persistence.redis;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryRedisKeyValueStore implements RedisKeyValueStore {

  private final ConcurrentHashMap<String, String> values = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Instant> expiresAt = new ConcurrentHashMap<>();

  @Override
  public Optional<String> get(String key) {
    Instant expiration = expiresAt.get(key);
    if (expiration != null && expiration.isBefore(Instant.now())) {
      values.remove(key);
      expiresAt.remove(key);
      return Optional.empty();
    }
    return Optional.ofNullable(values.get(key));
  }

  @Override
  public void set(String key, String value, Duration ttl) {
    values.put(key, value);
    if (ttl != null) {
      expiresAt.put(key, Instant.now().plus(ttl));
    }
  }

  public Map<String, String> snapshot() {
    return Map.copyOf(values);
  }
}
