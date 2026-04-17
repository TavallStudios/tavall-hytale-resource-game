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
  await writeFile(filePath, `${formatStructuredText(value)}\n`, "utf8");
}

export async function writeText(filePath, value) {
  await mkdir(path.dirname(filePath), { recursive: true });
  await writeFile(filePath, value.endsWith("\n") ? value : `${value}\n`, "utf8");
}

export function formatStructuredText(value, indent = 0) {
  const prefix = " ".repeat(indent);
  if (value == null) {
    return `${prefix}null`;
  }
  if (typeof value === "string") {
    return `${prefix}${value}`;
  }
  if (typeof value === "number" || typeof value === "boolean" || typeof value === "bigint") {
    return `${prefix}${String(value)}`;
  }
  if (Array.isArray(value)) {
    if (value.length === 0) {
      return `${prefix}[]`;
    }
    return value
      .map((entry) => {
        if (entry == null || typeof entry !== "object" || Array.isArray(entry)) {
          return `${prefix}- ${formatStructuredText(entry, 0).trimStart()}`;
        }
        return `${prefix}-\n${formatStructuredText(entry, indent + 2)}`;
      })
      .join("\n");
  }
  const entries = Object.entries(value);
  if (entries.length === 0) {
    return `${prefix}{}`;
  }
  return entries
    .map(([key, entry]) => {
      if (entry == null || typeof entry !== "object") {
        return `${prefix}${key}: ${formatStructuredText(entry, 0).trimStart()}`;
      }
      if (Array.isArray(entry) && entry.length === 0) {
        return `${prefix}${key}: []`;
      }
      return `${prefix}${key}:\n${formatStructuredText(entry, indent + 2)}`;
    })
    .join("\n");
}

export function printStructured(value, useError = false) {
  const output = `${formatStructuredText(value)}\n`;
  if (useError) {
    process.stderr.write(output);
    return;
  }
  process.stdout.write(output);
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

export async function aimAtPosition(bot, targetPosition, settleDelayMs = 250) {
  const snapshot = captureWorldSnapshot(bot, 4);
  if (!snapshot?.position || !targetPosition) {
    throw new Error("Unable to aim without both player and target positions.");
  }
  const dx = targetPosition.x - snapshot.position.x;
  const dy = targetPosition.y - snapshot.position.y;
  const dz = targetPosition.z - snapshot.position.z;
  const horizontalDistance = Math.sqrt((dx * dx) + (dz * dz));
  const yaw = Math.atan2(-dx, dz) * (180 / Math.PI);
  const pitch = -Math.atan2(dy, Math.max(horizontalDistance, 0.0001)) * (180 / Math.PI);
  bot.look(yaw, pitch, 0);
  if (settleDelayMs > 0) {
    await delay(settleDelayMs);
  }
}

export async function walkToApproxPosition(bot, targetPosition, options = {}) {
  const {
    stepSize = 0.9,
    maxSteps = 48,
    settleDelayMs = 120
  } = options;
  let snapshot = captureWorldSnapshot(bot, 4);
  if (!snapshot?.position || !targetPosition) {
    throw new Error("Unable to walk without both player and target positions.");
  }
  for (let index = 0; index < maxSteps; index += 1) {
    const current = snapshot.position;
    const dx = targetPosition.x - current.x;
    const dy = targetPosition.y - current.y;
    const dz = targetPosition.z - current.z;
    const distance = Math.sqrt((dx * dx) + (dy * dy) + (dz * dz));
    if (distance <= stepSize) {
      break;
    }
    const ratio = stepSize / distance;
    const nextPosition = {
      x: current.x + (dx * ratio),
      y: current.y + (dy * ratio),
      z: current.z + (dz * ratio)
    };
    await aimAtPosition(bot, targetPosition, 0);
    bot.move({ absolutePosition: nextPosition });
    if (settleDelayMs > 0) {
      await delay(settleDelayMs);
    }
    snapshot = captureWorldSnapshot(bot, 4);
    if (!snapshot?.position) {
      throw new Error("Lost player position during walk simulation.");
    }
  }
  return snapshot;
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
      await writeText(
        path.join(outputDir, "transcript.txt"),
        [
          `generatedAt=${new Date().toISOString()}`,
          "enabled=false",
          `mode=${mode}`,
          "note=Full bot transcript disabled by default. Set RESOURCE_GAME_BOT_TRACE=full to capture the raw trace."
        ].join("\n")
      );
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
