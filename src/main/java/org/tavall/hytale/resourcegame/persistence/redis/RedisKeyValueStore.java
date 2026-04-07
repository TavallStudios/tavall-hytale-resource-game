package org.tavall.hytale.resourcegame.persistence.redis;

import java.time.Duration;
import java.util.Optional;

public interface RedisKeyValueStore {

  Optional<String> get(String key);

  void set(String key, String value, Duration ttl);
}
