package org.tavall.hytale.resourcegame.app;

import java.time.Instant;

/**
 * Minimal runtime/test logger for deterministic integration traces.
 */
public final class Log {

  private Log() {
  }

  public static void info(String message) {
    System.out.println("[INFO][" + Instant.now() + "] " + message);
  }

  public static void warn(String message) {
    System.out.println("[WARN][" + Instant.now() + "] " + message);
  }

  public static void error(String message) {
    System.err.println("[ERROR][" + Instant.now() + "] " + message);
  }
}
