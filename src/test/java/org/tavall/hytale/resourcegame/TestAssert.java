package org.tavall.hytale.resourcegame;

public final class TestAssert {

  private TestAssert() {
  }

  public static void isTrue(boolean condition, String message) {
    if (!condition) {
      throw new IllegalStateException(message);
    }
  }

  public static void equalsInt(int expected, int actual, String message) {
    if (expected != actual) {
      throw new IllegalStateException(message + " expected=" + expected + " actual=" + actual);
    }
  }

  public static void equalsText(String expected, String actual, String message) {
    if (expected == null && actual == null) {
      return;
    }
    if (expected != null && expected.equals(actual)) {
      return;
    }
    throw new IllegalStateException(message + " expected=" + expected + " actual=" + actual);
  }
}
