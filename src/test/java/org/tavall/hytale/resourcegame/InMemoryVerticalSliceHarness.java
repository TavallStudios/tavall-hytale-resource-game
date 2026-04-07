package org.tavall.hytale.resourcegame;

import java.time.ZoneId;
import java.util.UUID;
import org.tavall.hytale.resourcegame.app.KingdomPrototypeKernel;
import org.tavall.hytale.resourcegame.app.Log;
import org.tavall.hytale.resourcegame.domain.player.PlayerStateBundle;
import org.tavall.hytale.resourcegame.domain.resource.ResourceType;
import org.tavall.hytale.resourcegame.runtime.HytaleAssetId;
import org.tavall.hytale.resourcegame.runtime.InMemoryHytaleRuntimeGateway;
import org.tavall.hytale.resourcegame.runtime.WorldPosition;
import org.tavall.hytale.resourcegame.support.HytaleBotHarness;

public final class InMemoryVerticalSliceHarness {

  private InMemoryVerticalSliceHarness() {
  }

  public static void main(String[] args) {
    InMemoryHytaleRuntimeGateway runtimeGateway = new InMemoryHytaleRuntimeGateway();
    KingdomPrototypeKernel kernel = KingdomPrototypeKernel.createInMemory(
        runtimeGateway,
        ZoneId.of("America/Los_Angeles")
    );

    UUID playerId = UUID.randomUUID();
    HytaleBotHarness bot = new HytaleBotHarness(
        kernel,
        runtimeGateway,
        playerId,
        "slice-bot",
        ZoneId.of("America/Los_Angeles"),
        "127.0.0.1",
        new WorldPosition("overworld", 11, 65, 11)
    );

    PlayerStateBundle bundle = bot.join();
    TestAssert.isTrue(bundle.gameState().castleId() != null, "castle id missing");
    TestAssert.equalsText("overworld", bundle.gameState().castleLocation().worldId(), "castle world mismatch");

    bot.lookAtOwnCastle();
    TestAssert.isTrue(bot.pollCastleSelection(), "near/look castle selection did not open UI");
    TestAssert.equalsText("castle-main", bot.lastOpenedUiId().orElseThrow(), "castle UI id mismatch");

    TestAssert.isTrue(bot.runCommand("/kd interior").success(), "interior command failed");
    TestAssert.isTrue(bot.currentWorld().startsWith("interior-"), "player did not move into interior world");

    long anchorCount = runtimeGateway.entitySnapshot().values().stream()
        .filter(entity -> entity.assetId() == HytaleAssetId.DISPLAY_CITIZEN_ANCHOR
            || entity.assetId() == HytaleAssetId.DISPLAY_TROOP_ANCHOR)
        .count();
    TestAssert.equalsInt(2, (int) anchorCount, "interior anchor entity count mismatch");

    TestAssert.isTrue(bot.runCommand("/kd citizens add 3").success(), "citizens add command failed");
    TestAssert.isTrue(bot.runCommand("/kd troops set 2").success(), "troops set command failed");
    TestAssert.isTrue(bot.runCommand("/kd resources set wood 200").success(), "resources set command failed");
    TestAssert.isTrue(bot.runCommand("/kd ui upgrade").success(), "ui upgrade command failed");
    TestAssert.equalsText("citizen-troop-upgrade", bot.lastOpenedUiId().orElseThrow(), "upgrade UI id mismatch");

    PlayerStateBundle updated = bot.session().orElseThrow();
    TestAssert.equalsInt(9, updated.gameState().citizenCount(), "citizen count mismatch");
    TestAssert.equalsInt(2, updated.gameState().troopCount(), "troop count mismatch");
    TestAssert.equalsInt(200, updated.gameState().resources().get(ResourceType.WOOD), "wood amount mismatch");

    Log.info("InMemoryVerticalSliceHarness passed");
  }
}
