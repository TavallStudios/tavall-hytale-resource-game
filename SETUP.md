# Setup Notes

## Prerequisites
- JDK 25 installed (`JAVA_HOME` should point to `C:\Program Files\Java\jdk-25`).
- No external service is required for local harness runs.

## Build
- `./mvnw.cmd test`

## Harness Tests (Executed as integration-style mains)
- `java -cp target/test-classes;target/classes org.tavall.hytale.resourcegame.InMemoryVerticalSliceHarness`
- `java -cp target/test-classes;target/classes org.tavall.hytale.resourcegame.RedisFirstGatewayHarness`
- `java -cp target/test-classes;target/classes org.tavall.hytale.resourcegame.LiveBotHarnessScenario`

## Notes
- Current first-join castle placement is temporary and intentionally spawns at the player join position.
- Runtime visuals are placeholders using Hytale asset identifiers designed for future replacement.
- Postgres JDBC store is implemented for production wiring; local harness defaults to in-memory persistence adapters.
- Asynchronous hydration/persistence uses `AsyncTask` virtual-thread execution.
