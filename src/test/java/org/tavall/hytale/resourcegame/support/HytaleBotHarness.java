package org.tavall.hytale.resourcegame.support;

import java.time.ZoneId;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import org.tavall.hytale.resourcegame.app.KingdomPrototypeKernel;
import org.tavall.hytale.resourcegame.app.Log;
import org.tavall.hytale.resourcegame.command.CommandResult;
import org.tavall.hytale.resourcegame.domain.player.PlayerStateBundle;
import org.tavall.hytale.resourcegame.runtime.HytaleAssetId;
import org.tavall.hytale.resourcegame.runtime.InMemoryHytaleRuntimeGateway;
import org.tavall.hytale.resourcegame.runtime.RuntimeEntity;
import org.tavall.hytale.resourcegame.runtime.WorldPosition;

public class HytaleBotHarness {

  private final KingdomPrototypeKernel kernel;
  private final InMemoryHytaleRuntimeGateway runtimeGateway;
  private final UUID playerId;
  private final String playerName;
  private final ZoneId timezone;
  private final String rawIp;
  private final WorldPosition joinPosition;

  public HytaleBotHarness(
      KingdomPrototypeKernel kernel,
      InMemoryHytaleRuntimeGateway runtimeGateway,
      UUID playerId,
      String playerName,
      ZoneId timezone,
      String rawIp,
      WorldPosition joinPosition
  ) {
    this.kernel = kernel;
    this.runtimeGateway = runtimeGateway;
    this.playerId = playerId;
    this.playerName = playerName;
    this.timezone = timezone;
    this.rawIp = rawIp;
    this.joinPosition = joinPosition;
  }

  public PlayerStateBundle join() {
    Log.info("Bot joining: " + playerName + " / " + playerId);
    PlayerStateBundle bundle = kernel.initializePlayer(playerId, playerName, timezone, rawIp, joinPosition).join();
    Log.info("Join complete. Castle=" + bundle.gameState().castleId());
    return bundle;
  }

  public void lookAtOwnCastle() {
    RuntimeEntity castle = runtimeGateway.entitySnapshot().values().stream()
        .filter(entity -> entity.assetId() == HytaleAssetId.CASTLE_STONE_TIER_ONE)
        .max(Comparator.comparing(RuntimeEntity::entityId))
        .orElseThrow(() -> new IllegalStateException("No castle entity found"));
    runtimeGateway.setPlayerLookTarget(playerId, castle.entityId());
    Log.info("Bot looking at castle entity " + castle.entityId());
  }

  public boolean pollCastleSelection() {
    boolean opened = kernel.pollCastleInteraction(playerId);
    Log.info("Castle selection poll opened UI=" + opened);
    return opened;
  }

  public CommandResult runCommand(String rawCommand) {
    Log.info("Bot command => " + rawCommand);
    CommandResult result = kernel.runCommand(playerId, rawCommand);
    Log.info("Command result => " + result.success() + " :: " + result.message());
    return result;
  }

  public Optional<String> lastOpenedUiId() {
    return runtimeGateway.lastOpenedUi(playerId).map(screen -> screen.screenId());
  }

  public String currentWorld() {
    return runtimeGateway.currentWorld(playerId);
  }

  public Optional<PlayerStateBundle> session() {
    return kernel.session(playerId);
  }
}
