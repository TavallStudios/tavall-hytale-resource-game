import { existsSync } from "node:fs";
import { mkdir, writeFile } from "node:fs/promises";
import path from "node:path";
import { pathToFileURL } from "node:url";

export function delay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

export function resolveBotClientModuleUrl() {
  const explicitClientPath = process.env.HYTALE_BOT_CLIENT ?? null;
  const localClientPath = path.resolve(process.cwd(), "packages", "client", "dist", "index.js");
  const monorepoClientPath = path.resolve(
    process.cwd(),
    "..",
    "..",
    "tavall-java-game-tools",
    "hytale-bots",
    "packages",
    "client",
    "dist",
    "index.js"
  );

  const resolvedClientPath =
    (explicitClientPath && existsSync(explicitClientPath) && explicitClientPath) ||
    (existsSync(localClientPath) && localClientPath) ||
    (existsSync(monorepoClientPath) && monorepoClientPath);

  if (!resolvedClientPath) {
    throw new Error("Unable to locate bot client build. Set HYTALE_BOT_CLIENT to the dist index.js path.");
  }

  return pathToFileURL(resolvedClientPath).href;
}

export async function writeJson(filePath, value) {
  await mkdir(path.dirname(filePath), { recursive: true });
  await writeFile(filePath, `${JSON.stringify(value, null, 2)}\n`, "utf8");
}

export async function waitForServerMessage(bot, predicate, timeoutMs, label) {
  return bot.waitForServerMessageMatching(predicate, timeoutMs, label);
}

export async function aimAtGround(bot, yaw = 0, pitch = 70, roll = 0, settleDelayMs = 500) {
  bot.look(yaw, pitch, roll);
  if (settleDelayMs > 0) {
    await delay(settleDelayMs);
  }
}

export async function focusTargetWithSweep(bot, options) {
  const {
    command = "/kingdom focus",
    expectedText,
    label = "focus target",
    timeoutPerAttemptMs = 1_250,
    settleDelayMs = 350,
    attempts = [
      { yaw: 0, pitch: 0, roll: 0 },
      { yaw: 45, pitch: 0, roll: 0 },
      { yaw: 90, pitch: 0, roll: 0 },
      { yaw: 135, pitch: 0, roll: 0 },
      { yaw: 180, pitch: 0, roll: 0 },
      { yaw: -135, pitch: 0, roll: 0 },
      { yaw: -90, pitch: 0, roll: 0 },
      { yaw: -45, pitch: 0, roll: 0 },
      { yaw: 0, pitch: -12, roll: 0 },
      { yaw: 180, pitch: -12, roll: 0 },
      { yaw: 90, pitch: -12, roll: 0 },
      { yaw: -90, pitch: -12, roll: 0 }
    ]
  } = options;

  const normalizedExpected = expectedText.toLowerCase();
  for (const attempt of attempts) {
    bot.look(attempt.yaw, attempt.pitch, attempt.roll ?? 0);
    await delay(settleDelayMs);
    bot.chat(command);
    try {
      await waitForServerMessage(
        bot,
        (message) => message.toLowerCase().includes(normalizedExpected),
        timeoutPerAttemptMs,
        label
      );
      return attempt;
    } catch {
    }
  }

  throw new Error(`Unable to focus ${label}`);
}

export function createTraceSession(bot, outputDir) {
  const mode = (process.env.RESOURCE_GAME_BOT_TRACE ?? "compact").toLowerCase();
  const enabled = mode === "full" || mode === "trace" || mode === "debug";

  return {
    enabled,
    async enable() {
      if (enabled) {
        await bot.trace.enable({ outputDir });
      }
    },
    async flush() {
      if (enabled) {
        await bot.trace.flush(outputDir);
        return;
      }
      await writeJson(path.join(outputDir, "transcript.json"), {
        enabled: false,
        mode,
        generatedAt: new Date().toISOString(),
        note: "Full bot transcript disabled by default. Set RESOURCE_GAME_BOT_TRACE=full to capture the raw trace."
      });
    }
  };
}

export async function waitForPageOrNull(bot, pageKey, timeoutMs) {
  try {
    return await bot.waitForPage(pageKey, timeoutMs);
  } catch {
    return null;
  }
}

export function captureWorldSnapshot(bot, nearbyRadius = 12) {
  if (typeof bot.getWorldSnapshot === "function") {
    return bot.getWorldSnapshot(nearbyRadius);
  }

  const self = typeof bot.getSelfEntity === "function" ? bot.getSelfEntity() : null;
  const nearbyEntities = typeof bot.getNearbyEntities === "function"
    ? bot.getNearbyEntities(nearbyRadius, self?.position ?? null, false).map((entry) => ({
      id: entry.entity?.id ?? null,
      distance: entry.distance ?? null,
      position: entry.entity?.position ?? null,
      health: typeof bot.getStatByName === "function" ? bot.getStatByName("health", entry.entity?.id ?? undefined) : null
    }))
    : [];

  return {
    clientId: bot.clientId ?? null,
    position: self?.position ?? null,
    bodyOrientation: self?.bodyOrientation ?? null,
    lookOrientation: self?.lookOrientation ?? null,
    movementStates: self?.movementStates ?? null,
    health: typeof bot.getStatByName === "function" ? bot.getStatByName("health") : null,
    inventory: typeof bot.getInventorySnapshot === "function" ? bot.getInventorySnapshot() : null,
    statValues: [],
    entityCount: bot.world?.entities?.size ?? nearbyEntities.length,
    statTypeNames: [],
    nearbyEntities,
    worldHeight: bot.world?.worldSettings?.worldHeight ?? null,
    viewRadius: bot.world?.viewRadius ?? null
  };
}

export function distanceBetweenPositions(left, right) {
  const dx = left.x - right.x;
  const dy = left.y - right.y;
  const dz = left.z - right.z;
  return Math.sqrt((dx * dx) + (dy * dy) + (dz * dz));
}

export function findNearbyEntityByPosition(snapshot, expectedPosition, tolerance = 1.1) {
  const nearbyEntities = snapshot?.nearbyEntities ?? [];
  return nearbyEntities.find((entity) => {
    if (!entity?.position) {
      return false;
    }
    return distanceBetweenPositions(entity.position, expectedPosition) <= tolerance;
  }) ?? null;
}

export async function waitForWorldSnapshot(bot, predicate, timeoutMs, label, nearbyRadius = 16) {
  const startedAt = Date.now();
  while ((Date.now() - startedAt) < timeoutMs) {
    const snapshot = captureWorldSnapshot(bot, nearbyRadius);
    if (predicate(snapshot)) {
      return snapshot;
    }
    await delay(150);
  }
  throw new Error(`Timed out waiting for ${label}`);
}

export async function waitForPage(bot, timeoutMs) {
  if (bot.ui.currentPage) {
    return bot.ui.currentPage;
  }
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => {
      bot.off("page", onPage);
      reject(new Error(`Timed out waiting for page after ${timeoutMs}ms`));
    }, timeoutMs);

    const onPage = (page) => {
      clearTimeout(timer);
      bot.off("page", onPage);
      resolve(page);
    };

    bot.on("page", onPage);
  });
}

export async function ensureOp(bot, username, timeoutMs = 3000) {
  if (!username) {
    return;
  }
  bot.chat(`/op ${username}`);
  try {
    await bot.waitForServerMessageMatching(
      (message) => message.toLowerCase().includes("op") || message.toLowerCase().includes("operator"),
      timeoutMs,
      "op-confirmation"
    );
  } catch {
  }
}

export async function ensureBotBaseline(bot, assertions, options = {}) {
  const {
    username,
    op = true,
    requireHealth = true,
    requireInventory = true,
    nearbyRadius = 12,
    settleDelayMs = 2000
  } = options;

  await bot.waitForReady(15_000);
  assertions.push("connected");
  await bot.waitForWorldActivity(10_000);
  assertions.push("world-joined");

  if (settleDelayMs > 0) {
    await delay(settleDelayMs);
  }

  if (op && username) {
    await ensureOp(bot, username);
    assertions.push("op-requested");
  }

  if (typeof bot.waitForSelfEntity === "function") {
    await bot.waitForSelfEntity(10_000);
    assertions.push("self-entity");
  } else {
    assertions.push("self-entity-unavailable");
  }

  let health = null;
  if (requireHealth) {
    if (typeof bot.waitForStat === "function") {
      health = await bot.waitForStat("health", 10_000).catch(() => null);
    } else if (typeof bot.getStatByName === "function") {
      health = bot.getStatByName("health");
    }
    if (health == null) {
      assertions.push("health-stat-unavailable");
    } else {
      assertions.push("health-stat");
    }
  }

  let inventory = null;
  if (requireInventory) {
    if (typeof bot.waitForInventory === "function") {
      inventory = await bot.waitForInventory(10_000).catch(() => null);
    } else if (typeof bot.getInventorySnapshot === "function") {
      inventory = bot.getInventorySnapshot();
    }
    if (inventory == null) {
      assertions.push("inventory-unavailable");
    } else {
      assertions.push("inventory-synced");
    }
  }

  const snapshot = captureWorldSnapshot(bot, nearbyRadius);
  assertions.push("world-scan");

  return {
    health,
    inventory,
    snapshot
  };
}

