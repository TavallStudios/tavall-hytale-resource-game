package org.tavall.hytale.resourcegame;

import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.tavall.hytale.resourcegame.app.Log;
import org.tavall.hytale.resourcegame.cache.PlayerHotStateCache;
import org.tavall.hytale.resourcegame.domain.player.PlayerJoinRequest;
import org.tavall.hytale.resourcegame.domain.player.PlayerStateBundle;
import org.tavall.hytale.resourcegame.persistence.PlayerStateStore;
import org.tavall.hytale.resourcegame.persistence.redis.InMemoryRedisKeyValueStore;
import org.tavall.hytale.resourcegame.persistence.redis.RedisPlayerStateStore;
import org.tavall.hytale.resourcegame.runtime.WorldPosition;
import org.tavall.hytale.resourcegame.service.PlayerStateFactory;
import org.tavall.hytale.resourcegame.service.PlayerStateGateway;
import org.tavall.hytale.resourcegame.service.PopulationMetadataCodec;
import org.tavall.hytale.resourcegame.support.CountingPlayerStateStore;
import org.tavall.hytale.resourcegame.support.EmptyPlayerStateStore;
import org.tavall.hytale.resourcegame.support.LatchPlayerStateStore;

public final class RedisFirstGatewayHarness {

  private RedisFirstGatewayHarness() {
  }

  public static void main(String[] args) {
    Log.info("RedisFirstGatewayHarness starting");
    UUID playerId = UUID.randomUUID();
    PlayerJoinRequest joinRequest = new PlayerJoinRequest(
        playerId,
        "redis-bot",
        ZoneId.of("America/Los_Angeles"),
        "hashed-ip",
        new WorldPosition("overworld", 0, 64, 0)
    );

    PlayerStateFactory factory = new PlayerStateFactory();
    PlayerStateBundle cached = factory.createFirstJoin(joinRequest, Instant.now());
    RedisPlayerStateStore redisStore = new RedisPlayerStateStore(new InMemoryRedisKeyValueStore());
    redisStore.save(cached);

    CountingPlayerStateStore postgresStore = new CountingPlayerStateStore(new EmptyPlayerStateStore());
    PlayerStateGateway gateway = new PlayerStateGateway(
        new PlayerHotStateCache(20),
        redisStore,
        postgresStore,
        factory,
        new PopulationMetadataCodec()
    );

    PlayerStateBundle hydrated = gateway.hydrate(joinRequest).join();
    TestAssert.isTrue(hydrated != null, "hydrate returned null");
    TestAssert.equalsInt(0, postgresStore.loadCalls(), "postgres load should not be called when redis has state");

    LatchPlayerStateStore latchPostgres = new LatchPlayerStateStore();
    PlayerStateStore redisNoop = new RedisPlayerStateStore(new InMemoryRedisKeyValueStore());
    PlayerStateGateway asyncGateway = new PlayerStateGateway(
        new PlayerHotStateCache(20),
        redisNoop,
        latchPostgres,
        factory,
        new PopulationMetadataCodec()
    );

    CompletableFuture<PlayerStateBundle> persistFuture = asyncGateway.persist(cached);
    TestAssert.isTrue(!persistFuture.isDone(), "persist should not complete before latch release");
    latchPostgres.release();
    TestAssert.isTrue(persistFuture.join() != null, "persist future returned null after release");
    Log.info("RedisFirstGatewayHarness passed");
  }
}
