package org.tavall.hytale.resourcegame;

import java.time.ZoneId;
import java.util.UUID;
import org.tavall.hytale.resourcegame.app.KingdomPrototypeKernel;
import org.tavall.hytale.resourcegame.app.Log;
import org.tavall.hytale.resourcegame.command.CommandResult;
import org.tavall.hytale.resourcegame.domain.player.PlayerStateBundle;
import org.tavall.hytale.resourcegame.domain.resource.ResourceType;
import org.tavall.hytale.resourcegame.persistence.PlayerStateStore;
import org.tavall.hytale.resourcegame.persistence.postgres.InMemoryPostgresPlayerStateStore;
import org.tavall.hytale.resourcegame.persistence.redis.InMemoryRedisKeyValueStore;
import org.tavall.hytale.resourcegame.persistence.redis.RedisPlayerStateStore;
import org.tavall.hytale.resourcegame.runtime.InMemoryHytaleRuntimeGateway;
import org.tavall.hytale.resourcegame.runtime.WorldPosition;
import org.tavall.hytale.resourcegame.support.HytaleBotHarness;

public final class LiveBotHarnessScenario {

  private LiveBotHarnessScenario() {
  }

  public static void main(String[] args) {
    PlayerStateStore sharedRedisStore = new RedisPlayerStateStore(new InMemoryRedisKeyValueStore());
    PlayerStateStore sharedPostgresStore = new InMemoryPostgresPlayerStateStore();

    InMemoryHytaleRuntimeGateway runtimeA = new InMemoryHytaleRuntimeGateway();
    KingdomPrototypeKernel kernelA = KingdomPrototypeKernel.createWithStateStores(
        runtimeA,
        ZoneId.of("America/Los_Angeles"),
        sharedRedisStore,
        sharedPostgresStore
    );

    UUID playerId = UUID.randomUUID();
    HytaleBotHarness botA = new HytaleBotHarness(
        kernelA,
        runtimeA,
        playerId,
        "live-bot",
        ZoneId.of("America/Los_Angeles"),
        "10.0.0.9",
        new WorldPosition("overworld", 4, 72, 4)
    );

    botA.join();
    TestAssert.isTrue(botA.runCommand("/kingdom").success(), "kingdom command failed");
    TestAssert.isTrue(botA.runCommand("/kd resources add food 40").success(), "resource add command failed");
    TestAssert.isTrue(botA.runCommand("/kd citizens set 12").success(), "citizens set command failed");

    PlayerStateBundle beforeRestart = botA.session().orElseThrow();
    kernelA.playerStateGateway().persist(beforeRestart).join();

    InMemoryHytaleRuntimeGateway runtimeB = new InMemoryHytaleRuntimeGateway();
    KingdomPrototypeKernel kernelB = KingdomPrototypeKernel.createWithStateStores(
        runtimeB,
        ZoneId.of("America/Los_Angeles"),
        sharedRedisStore,
        sharedPostgresStore
    );

    HytaleBotHarness botB = new HytaleBotHarness(
        kernelB,
        runtimeB,
        playerId,
        "live-bot",
        ZoneId.of("America/Los_Angeles"),
        "10.0.0.9",
        new WorldPosition("overworld", 6, 72, 6)
    );

    PlayerStateBundle afterRestart = botB.join();
    TestAssert.equalsInt(160, afterRestart.gameState().resources().get(ResourceType.FOOD), "food should persist across runtime instances");
    TestAssert.equalsInt(12, afterRestart.gameState().citizenCount(), "citizen count should persist across runtime instances");

    CommandResult help = botB.runCommand("/kd debug help");
    TestAssert.isTrue(help.success(), "debug help command failed");
    Log.info("LiveBotHarnessScenario passed");
  }
}
