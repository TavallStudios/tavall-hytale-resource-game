package org.tavall.hytale.resourcegame.domain.resource;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Mutable inventory for the starter resource set used by the vertical slice.
 */
public class ResourceInventory {

  private final EnumMap<ResourceType, Integer> values = new EnumMap<>(ResourceType.class);

  public ResourceInventory() {
    for (ResourceType type : ResourceType.values()) {
      values.put(type, 0);
    }
  }

  public ResourceInventory(Map<ResourceType, Integer> initialValues) {
    this();
    Objects.requireNonNull(initialValues, "initialValues");
    for (Map.Entry<ResourceType, Integer> entry : initialValues.entrySet()) {
      set(entry.getKey(), entry.getValue());
    }
  }

  public int get(ResourceType type) {
    return values.getOrDefault(type, 0);
  }

  public void add(ResourceType type, int amount) {
    if (amount == 0) {
      return;
    }
    int nextValue = get(type) + amount;
    if (nextValue < 0) {
      throw new IllegalArgumentException("resource amount cannot be negative");
    }
    values.put(type, nextValue);
  }

  public void set(ResourceType type, int amount) {
    if (amount < 0) {
      throw new IllegalArgumentException("resource amount cannot be negative");
    }
    values.put(type, amount);
  }

  public boolean hasAtLeast(ResourceType type, int amount) {
    return get(type) >= amount;
  }

  public Map<ResourceType, Integer> snapshot() {
    return Map.copyOf(values);
  }

  public ResourceInventory copy() {
    return new ResourceInventory(values);
  }
}
